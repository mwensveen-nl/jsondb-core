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
package io.jsondb.tests;

import com.google.common.io.Files;
import io.jsondb.InvalidJsonDbApiUsageException;
import io.jsondb.JsonDBTemplate;
import io.jsondb.crypto.Default1Cipher;
import io.jsondb.crypto.ICipher;
import io.jsondb.tests.model.Instance;
import io.jsondb.tests.model.Site;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Junit tests for the remove() apis
 *
 * @version 1.0 08-Oct-2016
 */
public class RemoveTests {
    private static final String INSTANCES_JSON = "instances.json";
    @TempDir
    private File dbFilesFolder;
    private JsonDBTemplate jsonDBTemplate = null;

    @BeforeEach
    public void setUp() throws Exception {
        Files.copy(new File("src/test/resources/dbfiles/instances.json"), new File(dbFilesFolder, INSTANCES_JSON));
        ICipher cipher = new Default1Cipher("1r8+24pibarAWgS85/Heeg==");
        jsonDBTemplate = new JsonDBTemplate(dbFilesFolder.getAbsolutePath(), "io.jsondb.tests.model", cipher);
    }

    /**
     * Test to remove a single non-existing object from a collection
     */
    @Test
    public void testRemove_NonExistingObject() {
        Instance instance = new Instance();
        instance.setId("000012");

        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.remove(instance, Instance.class));
        assertEquals("Objects with Id 000012 not found in collection instances", exception.getMessage());
    }

    /**
     * Test to remove a null object from a collection
     */
    @Test
    public void testRemove_NullObject() {
        Object nullObject = null;
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.remove(nullObject, Instance.class));
        assertEquals("Null Object cannot be removed from DB", exception.getMessage());
    }

    /**
     * Test to remove a null object from a collection
     */
    @Test
    public void testRemove_NullObjectBatch() {
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.remove(null, Instance.class));
        assertEquals("Null Object batch cannot be removed from DB", exception.getMessage());
    }

    /**
     * Test to remove a object from a non-existent collection
     */
    @Test
    public void testRemove_FromNonExistingCollection() {
        Site s = new Site();
        s.setId("000012");

        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.remove(s, Site.class));
        assertEquals("Collection by name 'sites' not found. Create collection first.", exception.getMessage());
    }

    /**
     * Test to remove a object from a non-existent collection
     */
    @Test
    public void testRemoveBatch_FromNonExistingCollection() {
        Site s = new Site();
        s.setId("000012");
        List<Site> ss = new ArrayList<>();
        ss.add(s);

        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.remove(ss, "sites"));
        assertEquals("Collection by name 'sites' not found. Create collection first.", exception.getMessage());
    }

    /**
     * Test to remove a single object from a collection
     */
    @Test
    public void testRemove_ValidObjectWithClass() {
        List<Instance> instances = jsonDBTemplate.getCollection(Instance.class);
        int size = instances.size();

        Instance instance = new Instance();
        instance.setId("05");

        Instance removedObject = jsonDBTemplate.remove(instance, Instance.class);

        instances = jsonDBTemplate.getCollection(Instance.class);
        assertNotNull(instances);
        assertEquals(size - 1, instances.size());
        assertNotNull(removedObject);
        assertEquals("05", removedObject.getId());
    }

    /**
     * Test to remove a single object from a collection
     */
    @Test
    public void testRemove_ValidObjectWithoutClass() {
        List<Instance> instances = jsonDBTemplate.getCollection(Instance.class);
        int size = instances.size();

        Instance instance = new Instance();
        instance.setId("05");

        Instance removedObject = jsonDBTemplate.remove(instance);

        instances = jsonDBTemplate.getCollection(Instance.class);
        assertNotNull(instances);
        assertEquals(size - 1, instances.size());
        assertNotNull(removedObject);
        assertEquals("05", removedObject.getId());
    }

    /**
     * Test to remove a batch of objects from collection
     */
    @Test
    public void testRemove_BatchOfObjects() {
        List<Instance> instances = jsonDBTemplate.getCollection(Instance.class);
        int size = instances.size();

        List<Instance> batch = new ArrayList<>();
        for (int i = 1; i < 3; i++) {
            Instance e = new Instance();
            e.setId(String.format("%02d", i));
            batch.add(e);
        }

        List<Instance> removedObjects = jsonDBTemplate.remove(batch, Instance.class);

        instances = jsonDBTemplate.getCollection(Instance.class);
        assertNotNull(instances);
        assertEquals(size - 2, instances.size());
        assertNotNull(removedObjects);
        assertEquals(2, removedObjects.size());
    }
}
