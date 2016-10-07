/*
 * Copyright (c) 2016 Farooq Khan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jsondb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.jsondb.crypto.CryptoUtil;
import io.jsondb.crypto.ICipher;
import io.jsondb.io.JsonFileLockException;
import io.jsondb.io.JsonReader;
import io.jsondb.io.JsonWriter;
import io.jsondb.query.CollectionSchemaUpdate;
import io.jsondb.query.Update;

/**
 * @version 1.0 25-Sep-2016
 */
public class JsonDBTemplate implements JsonDBOperations {
  private Logger logger = LoggerFactory.getLogger(JsonDBTemplate.class);

  private JsonDBConfig dbConfig = null;
  private final boolean encrypted;
  private File lockFilesLocation;

  private Map<String, CollectionMetaData> cmdMap;
  private AtomicReference<Map<String, File>> fileObjectsRef = new AtomicReference<Map<String, File>>(new ConcurrentHashMap<String, File>());
  private AtomicReference<Map<String, Map<Object, ?>>> collectionsRef = new AtomicReference<Map<String, Map<Object, ?>>>(new ConcurrentHashMap<String, Map<Object, ?>>());
  private AtomicReference<Map<String, JXPathContext>> contextsRef = new AtomicReference<Map<String, JXPathContext>>(new ConcurrentHashMap<String, JXPathContext>());

  public JsonDBTemplate(String dbFilesLocationString, String baseScanPackage) {
    this(dbFilesLocationString, baseScanPackage, null, false, null);
  }

  public JsonDBTemplate(String dbFilesLocationString, String baseScanPackage, boolean compatibilityMode, Comparator<String> schemaComparator) {
    this(dbFilesLocationString, baseScanPackage, null, compatibilityMode, schemaComparator);
  }

  public JsonDBTemplate(String dbFilesLocationString, String baseScanPackage, ICipher cipher) {
    this(dbFilesLocationString, baseScanPackage, cipher, false, null);
  }

  public JsonDBTemplate(String dbFilesLocationString, String baseScanPackage, ICipher cipher, boolean compatibilityMode, Comparator<String> schemaComparator) {
    dbConfig = new JsonDBConfig(dbFilesLocationString, baseScanPackage, cipher, compatibilityMode, schemaComparator);
    this.encrypted = true;
    initialize();
  }

  private void initialize(){
    this.lockFilesLocation = new File(dbConfig.getDbFilesLocation(), "lock");
    if(!lockFilesLocation.exists()) {
      lockFilesLocation.mkdirs();
    }
    if (!dbConfig.getDbFilesLocation().exists()) {
      try {
        Files.createDirectory(dbConfig.getDbFilesPath());
      } catch (IOException e) {
        logger.error("DbFiles directory does not exist. Failed to create a new empty DBFiles directory {}", e);
        throw new InvalidJsonDbApiUsageException("DbFiles directory does not exist. Failed to create a new empty DBFiles directory " + dbConfig.getDbFilesLocationString());
      }
    } else if (dbConfig.getDbFilesLocation().isFile()) {
      throw new InvalidJsonDbApiUsageException("Specified DbFiles directory is actually a file cannot use it as a directory");
    }

    cmdMap = CollectionMetaData.builder(dbConfig);

    loadDB();

    // Auto-cleanup at shutdown
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        shutdown();
      }
    });
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#reLoadDB()
   */
  @Override
  public void reLoadDB() {
    loadDB();
  }

  private synchronized void loadDB() {
    for(String collectionName : cmdMap.keySet()) {
      File collectionFile = new File(dbConfig.getDbFilesLocation(), collectionName + ".json");
      if(collectionFile.exists()) {
        reloadCollection(collectionName);
      }
    }
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#reloadCollection(java.lang.String)
   */
  public void reloadCollection(String collectionName) {
    CollectionMetaData cmd = cmdMap.get(collectionName);
    cmd.getCollectionLock().writeLock().lock();
    try {
      File collectionFile = fileObjectsRef.get().get(collectionName);
      if(null == collectionFile) {
        // Lets create a file now
        collectionFile = new File(dbConfig.getDbFilesLocation(), collectionName + ".json");
        if(!collectionFile.exists()) {
          throw new InvalidJsonDbApiUsageException("Collection by name '" + collectionName + "' cannot be found at " + collectionFile.getAbsolutePath());
        }
        Map<String, File> fileObjectMap = fileObjectsRef.get();
        Map<String, File> newFileObjectmap = new ConcurrentHashMap<String, File>(fileObjectMap);
        newFileObjectmap.put(collectionName, collectionFile);
        fileObjectsRef.set(newFileObjectmap);
      }
      if (null != cmd && null != collectionFile) {
        Map<Object, ?> collection = loadCollection(collectionFile, collectionName, cmd);
        if (null != collection) {
          JXPathContext newContext = JXPathContext.newContext(collection.values());
          contextsRef.get().put(collectionName, newContext);
          collectionsRef.get().put(collectionName, collection);
        }
      }
    } finally {
      cmd.getCollectionLock().writeLock().unlock();
    }
  }

  private <T> Map<Object, T> loadCollection(File collectionFile, String collectionName, CollectionMetaData cmd) {
    @SuppressWarnings("unchecked")
    Class<T> entity = cmd.getClazz();
    Method getterMethodForId = cmd.getIdAnnotatedFieldGetterMethod();

    JsonReader jr = null;
    Map<Object, T> collection = new LinkedHashMap<Object, T>();

    String line = null;
    int lineNo = 1;
    try {
      jr = new JsonReader(dbConfig, collectionFile);

      while ((line = jr.readLine()) != null) {
        if (lineNo == 1) {
          SchemaVersion v = dbConfig.getObjectMapper().readValue(line, SchemaVersion.class);
          cmd.setActualSchemaVersion(v.getSchemaVersion());
        } else {
          T row = dbConfig.getObjectMapper().readValue(line, entity);
          Object id = Util.getIdForEntity(row, getterMethodForId);
          collection.put(id, row);
        }
        lineNo++;
      }
    } catch (JsonParseException je) {
      logger.error("Failed Json Parsing for file {} line {}", collectionFile.getName(), lineNo, je);
      return null;
    } catch (JsonMappingException jm) {
      logger.error("Failed Mapping Parsed Json to Entity {} for file {} line {}",
          entity.getSimpleName(), collectionFile.getName(), lineNo, jm);
      return null;
    } catch (CharacterCodingException ce) {
      logger.error("Unsupported Character Encoding in file {} expected Encoding {}",
          collectionFile.getName(), dbConfig.getCharset().displayName(), ce);
      return null;
    } catch (JsonFileLockException jfe) {
      logger.error("Failed to acquire lock for collection file {}", collectionFile.getName(), jfe);
      return null;
    } catch (FileNotFoundException fe) {
      logger.error("Collection file {} not found", collectionFile.getName(), fe);
      return null;
    } catch (IOException e) {
      logger.error("Some IO Exception reading the Json File {}", collectionFile.getName(), e);
      return null;
    } catch(Throwable t) {
      logger.error("Throwable Caught ", collectionFile.getName(), t);
      return null;
    } finally {
      if (null != jr) {
        jr.close();
      }
    }
    return collection;
  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#shutdown()
   */
  @Override
  public void shutdown() {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#addCollectionFileChangeListener(org.jsondb.CollectionFileChangeListener)
   */
  @Override
  public void addCollectionFileChangeListener(CollectionFileChangeListener listener) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#removeCollectionFileChangeListener(org.jsondb.CollectionFileChangeListener)
   */
  @Override
  public void removeCollectionFileChangeListener(CollectionFileChangeListener listener) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#createCollection(java.lang.Class)
   */
  @Override
  public <T> void createCollection(Class<T> entityClass) {
    createCollection(Util.determineCollectionName(entityClass));
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#createCollection(java.lang.String)
   */
  @Override
  public <T> void createCollection(String collectionName) {
    CollectionMetaData cmd = cmdMap.get(collectionName);
    if (null == cmd) {
      throw new InvalidJsonDbApiUsageException(
          "No class found with @Document Annotation and attribute collectionName as: " + collectionName);
    }
    @SuppressWarnings("unchecked")
    Map<Object, T> collection = (Map<Object, T>) collectionsRef.get().get(collectionName);
    if (null != collection) {
      throw new InvalidJsonDbApiUsageException("Collection by name '" + collectionName + "' already exists.");
    }

    cmd.getCollectionLock().writeLock().lock();

    // Some other thread might have created same collection when this thread reached this point
    if(collectionsRef.get().get(collectionName) != null) {
      return;
    }

    try {
      String collectionFileName = collectionName + ".json";
      File fileObject = new File(dbConfig.getDbFilesLocation(), collectionFileName);
      try {
        fileObject.createNewFile();
      } catch (IOException e) {
        logger.error("IO Exception creating the collection file {}", collectionFileName, e);
        throw new InvalidJsonDbApiUsageException("Unable to create a collection file for collection: " + collectionName);
      }

      if (Util.stampVersion(dbConfig, fileObject, cmd.getSchemaVersion())) {
        collection = new LinkedHashMap<Object, T>();
        collectionsRef.get().put(collectionName, collection);
        contextsRef.get().put(collectionName, JXPathContext.newContext(collection.values())) ;
        fileObjectsRef.get().put(collectionName, fileObject);
        cmd.setActualSchemaVersion(cmd.getSchemaVersion());
      } else {
        fileObject.delete();
        throw new JsonDBException("Failed to stamp version for collection: " + collectionName);
      }
    } finally {
      cmd.getCollectionLock().writeLock().unlock();
    }
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#dropCollection(java.lang.Class)
   */
  @Override
  public <T> void dropCollection(Class<T> entityClass) {
    dropCollection(Util.determineCollectionName(entityClass));
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#dropCollection(java.lang.String)
   */
  @Override
  public void dropCollection(String collectionName) {
    CollectionMetaData collectionMeta = cmdMap.get(collectionName);
    if(null == collectionMeta) {
      throw new InvalidJsonDbApiUsageException("Failed to find collection with name '" + collectionName + "'");
    }
    collectionMeta.getCollectionLock().writeLock().lock();
    try {
      if (!collectionsRef.get().containsKey(collectionName)) {
        throw new InvalidJsonDbApiUsageException("Collection by name '" + collectionName + "' not found.");
      }
      File toDelete = fileObjectsRef.get().get(collectionName);
      try {
        Files.deleteIfExists(toDelete.toPath());
      } catch (IOException e) {
        logger.error("IO Exception deleting the collection file {}", toDelete.getName(), e);
        throw new InvalidJsonDbApiUsageException("Unable to create a collection file for collection: " + collectionName);
      }
      //cmdMap.remove(collectionName); //Do not remove it from the CollectionMetaData Map.
      //Someone might want to re insert a new collection of this type.
      fileObjectsRef.get().remove(collectionName);
      collectionsRef.get().remove(collectionName);
      contextsRef.get().remove(collectionName);
    } finally {
      collectionMeta.getCollectionLock().writeLock().unlock();
    }
  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#updateCollectionSchema(org.jsondb.query.CollectionSchemaUpdate, java.lang.Class)
   */
  @Override
  public <T> void updateCollectionSchema(CollectionSchemaUpdate update,
      Class<T> entityClass) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#updateCollectionSchema(org.jsondb.query.CollectionSchemaUpdate, java.lang.String)
   */
  @Override
  public <T> void updateCollectionSchema(CollectionSchemaUpdate update,
      String collectionName) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#getCollectionNames()
   */
  @Override
  public Set<String> getCollectionNames() {
    return collectionsRef.get().keySet();
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#getCollectionName(java.lang.Class)
   */
  @Override
  public String getCollectionName(Class<?> entityClass) {
    return Util.determineCollectionName(entityClass);
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#getCollection(java.lang.Class)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> getCollection(Class<T> entityClass) {
    String collectionName = Util.determineCollectionName(entityClass);
    Map<Object, T> collection = (Map<Object, T>) collectionsRef.get().get(collectionName);
    if (null == collection) {
      createCollection(collectionName);
      collection = (Map<Object, T>) collectionsRef.get().get(collectionName);
    }

    CollectionMetaData cmd = cmdMap.get(collectionName);
    List<T> newCollection = new ArrayList<T>();
    for (T document : collection.values()) {
      Object obj = Util.deepCopy(document);
      if(encrypted && cmd.hasSecret() && null != obj) {
        CryptoUtil.decryptFields(obj, cmd, dbConfig.getCipher());
      }
      newCollection.add((T) obj);
    }
    return newCollection;
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#collectionExists(java.lang.Class)
   */
  @Override
  public <T> boolean collectionExists(Class<T> entityClass) {
    return collectionExists(Util.determineCollectionName(entityClass));
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#collectionExists(java.lang.String)
   */
  @Override
  public boolean collectionExists(String collectionName) {
    CollectionMetaData collectionMeta = cmdMap.get(collectionName);
    if(null == collectionMeta) {
      return false;
    }
    collectionMeta.getCollectionLock().readLock().lock();
    try {
      return collectionsRef.get().containsKey(collectionName);
    } finally {
      collectionMeta.getCollectionLock().readLock().unlock();
    }
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#isCollectionReadonly(java.lang.Class)
   */
  @Override
  public <T> boolean isCollectionReadonly(Class<T> entityClass) {
    return isCollectionReadonly(Util.determineCollectionName(entityClass));
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#isCollectionReadonly(java.lang.String)
   */
  @Override
  public <T> boolean isCollectionReadonly(String collectionName) {
    CollectionMetaData cmd = cmdMap.get(collectionName);
    return cmd.isReadOnly();
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#find(java.lang.String, java.lang.Class)
   */
  @Override
  public <T> List<T> find(String jxQuery, Class<T> entityClass) {
    return find(jxQuery, Util.determineCollectionName(entityClass));
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#find(java.lang.String, java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> find(String jxQuery, String collectionName) {
    CollectionMetaData cmd = cmdMap.get(collectionName);
    cmd.getCollectionLock().readLock().lock();
    try {
      JXPathContext context = contextsRef.get().get(collectionName);
      try {
        Iterator<T> resultItr = context.iterate(jxQuery);
        List<T> newCollection = new ArrayList<T>();
        while (resultItr.hasNext()) {
          T document = resultItr.next();
          Object obj = Util.deepCopy(document);
          if(encrypted && cmd.hasSecret() && null != obj) {
            CryptoUtil.decryptFields(obj, cmd, dbConfig.getCipher());
          }
          newCollection.add((T) obj);
        }
        return newCollection;
      } catch (JXPathNotFoundException e) {
        //TODO: Log the exception this is not a error state the XPATH query returned nothing.
        return null;
      }
    } finally {
      cmd.getCollectionLock().readLock().unlock();
    }
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#findAll(java.lang.Class)
   */
  @Override
  public <T> List<T> findAll(Class<T> entityClass) {
    return findAll(Util.determineCollectionName(entityClass));
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#findAll(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> findAll(String collectionName) {
    CollectionMetaData cmd = cmdMap.get(collectionName);
    if(null == cmd) {
      throw new InvalidJsonDbApiUsageException("Collection by name '" + collectionName + "' not found. Create collection first.");
    }
    cmd.getCollectionLock().readLock().lock();
    try {
      Map<Object, T> collection = (Map<Object, T>) collectionsRef.get().get(collectionName);
      if (null == collection) {
        throw new InvalidJsonDbApiUsageException("Collection by name '" + collectionName + "' not found. Create collection first.");
      }
      List<T> newCollection = new ArrayList<T>();
      for (T document : collection.values()) {
        T obj = (T)Util.deepCopy(document);
        if(encrypted && cmd.hasSecret() && null!=obj){
          CryptoUtil.decryptFields(obj, cmd, dbConfig.getCipher());
          newCollection.add(obj);
        } else{
          newCollection.add((T) obj);
        }
      }
      return newCollection;
    } finally {
      cmd.getCollectionLock().readLock().unlock();
    }
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#findById(java.lang.Object, java.lang.Class)
   */
  @Override
  public <T> T findById(Object id, Class<T> entityClass) {
    return findById(id, Util.determineCollectionName(entityClass));
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#findById(java.lang.Object, java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T findById(Object id, String collectionName) {
    CollectionMetaData collectionMeta = cmdMap.get(collectionName);
    if(null == collectionMeta) {
      throw new InvalidJsonDbApiUsageException("Collection by name '" + collectionName + "' not found. Create collection first.");
    }
    collectionMeta.getCollectionLock().readLock().lock();
    try {
      Map<Object, T> collection = (Map<Object, T>) collectionsRef.get().get(collectionName);
      if (null == collection) {
        throw new InvalidJsonDbApiUsageException("Collection by name '" + collectionName + "' not found. Create collection first.");
      }
      Object obj = Util.deepCopy(collection.get(id));
      if(encrypted && collectionMeta.hasSecret() && null != obj){
        CryptoUtil.decryptFields(obj, collectionMeta, dbConfig.getCipher());
      }
      return (T) obj;
    } finally {
      collectionMeta.getCollectionLock().readLock().unlock();
    }
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#findOne(java.lang.String, java.lang.Class)
   */
  @Override
  public <T> T findOne(String jxQuery, Class<T> entityClass) {
    return findOne(jxQuery, Util.determineCollectionName(entityClass));
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#findOne(java.lang.String, java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T findOne(String jxQuery, String collectionName) {
    CollectionMetaData collectionMeta = cmdMap.get(collectionName);
    if(null == collectionMeta) {
      throw new InvalidJsonDbApiUsageException("Collection by name '" + collectionName + "' not found. Create collection first");
    }
    collectionMeta.getCollectionLock().readLock().lock();
    try {
      if (!collectionsRef.get().containsKey(collectionName)) {
        throw new InvalidJsonDbApiUsageException("Collection by name '" + collectionName + "' not found. Create collection first");
      }
      JXPathContext context = contextsRef.get().get(collectionName);
      Iterator<T> resultItr = context.iterate(jxQuery);
      while (resultItr.hasNext()) {
        T document = resultItr.next();
        Object obj = Util.deepCopy(document);
        if(encrypted && collectionMeta.hasSecret() && null!= obj){
          CryptoUtil.decryptFields(obj, collectionMeta, dbConfig.getCipher());
        }
        return (T) obj; // Return the first element we find.
      }
      return null;
    } finally {
      collectionMeta.getCollectionLock().readLock().unlock();
    }
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#insert(java.lang.Object)
   */
  @Override
  public <T> void insert(Object objectToSave) {
    if (null == objectToSave) {
      throw new InvalidJsonDbApiUsageException("Null Object cannot be inserted into DB");
    }
    Util.ensureNotRestricted(objectToSave);
    insert(objectToSave, Util.determineEntityCollectionName(objectToSave));
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#insert(java.lang.Object, java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> void insert(Object objectToSave, String collectionName) {
    if (null == objectToSave) {
      throw new InvalidJsonDbApiUsageException("Null Object cannot be inserted into DB");
    }
    Util.ensureNotRestricted(objectToSave);
    Object objToSave = Util.deepCopy(objectToSave);
    CollectionMetaData cmd = cmdMap.get(collectionName);
    cmd.getCollectionLock().writeLock().lock();
    try {
      Map<Object, T> collection = (Map<Object, T>) collectionsRef.get().get(collectionName);
      if (null == collection) {
        throw new InvalidJsonDbApiUsageException("Collection by name '" + collectionName + "' not found. Create collection first");
      }
      Object id = Util.getIdForEntity(objectToSave, cmd.getIdAnnotatedFieldGetterMethod());
      if(encrypted && cmd.hasSecret()){
        CryptoUtil.encryptFields(objToSave, cmd, dbConfig.getCipher());
      }
      if (null == id) {
        id = Util.setIdForEntity(objToSave, cmd.getIdAnnotatedFieldSetterMethod());
      } else if (collection.containsKey(id)) {
        throw new InvalidJsonDbApiUsageException("Object already present in Collection. Use Update or Upsert operation instead of Insert");
      }

      JsonWriter jw;
      try {
        jw = new JsonWriter(dbConfig, cmd, collectionName, fileObjectsRef.get().get(collectionName));
      } catch (IOException ioe) {
        logger.error("Failed to obtain writer for " + collectionName, ioe);
        throw new JsonDBException("Failed to save " + collectionName, ioe);
      }

      boolean appendResult = jw.appendToJsonFile(collection.values(), objToSave);

      if(appendResult) {
        collection.put(Util.deepCopy(id), (T) objToSave);
      }
    } finally {
      cmd.getCollectionLock().writeLock().unlock();
    }
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#insert(java.util.Collection, java.lang.Class)
   */
  @Override
  public <T> void insert(Collection<? extends T> batchToSave, Class<T> entityClass) {
    insert(batchToSave, Util.determineCollectionName(entityClass));
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#insert(java.util.Collection, java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> void insert(Collection<? extends T> batchToSave, String collectionName) {
    CollectionMetaData collectionMeta = cmdMap.get(collectionName);
    collectionMeta.getCollectionLock().writeLock().lock();
    try {
      Map<Object, T> collection = (Map<Object, T>) collectionsRef.get().get(collectionName);
      if (null == collection) {
        throw new InvalidJsonDbApiUsageException("Collection by name '" + collectionName + "' not found. Create collection first");
      }
      CollectionMetaData cmd = cmdMap.get(collectionName);
      Set<Object> uniqueIds = new HashSet<Object>();
      Map<Object, T> newCollection = new LinkedHashMap<Object, T>();
      for (T o : batchToSave) {
        Object obj = Util.deepCopy(o);
        Object id = Util.getIdForEntity(obj, cmd.getIdAnnotatedFieldGetterMethod());
        if(encrypted && cmd.hasSecret()){
          CryptoUtil.encryptFields(obj, cmd, dbConfig.getCipher());
        }
        if (null == id) {
          id = Util.setIdForEntity(obj, cmd.getIdAnnotatedFieldSetterMethod());
        } else if (collection.containsKey(id)) {
          throw new InvalidJsonDbApiUsageException("Object already present in Collection. Use Update or Upsert operation instead of Insert");
        }
        if (!uniqueIds.add(id)) {
          throw new InvalidJsonDbApiUsageException("Duplicate object with id: " + id + " within the passed in parameter");
        }
        newCollection.put(Util.deepCopy(id), (T) obj);
      }

      JsonWriter jw;
      try {
        jw = new JsonWriter(dbConfig, cmd, collectionName, fileObjectsRef.get().get(collectionName));
      } catch (IOException ioe) {
        logger.error("Failed to obtain writer for " + collectionName, ioe);
        throw new JsonDBException("Failed to save " + collectionName, ioe);
      }
      boolean appendResult = jw.appendToJsonFile(collection.values(), newCollection.values());

      if(appendResult) {
        collection.putAll(newCollection);
      }
    } finally {
      collectionMeta.getCollectionLock().writeLock().unlock();
    }
  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#save(java.lang.Object, java.lang.Class)
   */
  @Override
  public <T> void save(Object objectToSave, Class<T> entityClass) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see io.jsondb.JsonDBOperations#save(java.lang.Object, java.lang.String)
   */
  @Override
  public <T> void save(Object objectToSave, String collectionName) {
    // TODO Auto-generated method stub

  }
  
  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#findAndRemove(java.lang.String, java.lang.Class)
   */
  @Override
  public <T> int findAndRemove(String jxQuery, Class<T> entityClass) {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#findAndRemove(java.lang.String, java.lang.Class, java.lang.String)
   */
  @Override
  public <T> int findAndRemove(String jxQuery, Class<T> entityClass,
      String collectionName) {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#remove(java.lang.Object, java.lang.Class)
   */
  @Override
  public <T> int remove(Object object, Class<T> entityClass) {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#remove(java.lang.Object, java.lang.String)
   */
  @Override
  public <T> int remove(Object object, String collectionName) {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#remove(java.util.Collection, java.lang.Class)
   */
  @Override
  public <T> int remove(Collection<? extends T> batchToRemove,
      Class<T> entityClass) {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#remove(java.util.Collection, java.lang.String)
   */
  @Override
  public <T> int remove(Collection<? extends T> batchToRemove,
      String collectionName) {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#findAndModify(java.lang.String, org.jsondb.query.Update, java.lang.Class)
   */
  @Override
  public <T> int findAndModify(String jxQuery, Update update,
      Class<T> entityClass) {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#findAndModify(java.lang.String, org.jsondb.query.Update, java.lang.String)
   */
  @Override
  public <T> int findAndModify(String jxQuery, Update update,
      String collectionName) {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#upsert(java.lang.Object)
   */
  @Override
  public <T> void upsert(Object objectToSave) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#upsert(java.lang.Object, java.lang.String)
   */
  @Override
  public <T> void upsert(Object objectToSave, String collectionName) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#upsert(java.util.Collection, java.lang.Class)
   */
  @Override
  public <T> void upsert(Collection<? extends T> batchToSave,
      Class<T> entityClass) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#upsert(java.util.Collection, java.lang.String)
   */
  @Override
  public <T> void upsert(Collection<? extends T> batchToSave,
      String collectionName) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#backup(java.lang.String)
   */
  @Override
  public void backup(String backupPath) {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see org.jsondb.JsonDBOperations#restore(java.lang.String, boolean)
   */
  @Override
  public void restore(String restorePath, boolean merge) {
    // TODO Auto-generated method stub

  }
}