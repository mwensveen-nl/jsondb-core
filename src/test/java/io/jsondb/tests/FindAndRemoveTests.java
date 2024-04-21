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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @version 1.0 11-Oct-2016
 */

public class FindAndRemoveTests {
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
        String jxQuery = String.format("/.[id='%s']", "12");
        Instance i = jsonDBTemplate.findAndRemove(jxQuery, Instance.class);

        assertNull(i);
    }

    /**
     * Test to remove a null object from a collection
     */
    @Test
    public void testRemove_NullObject() {
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.findAndRemove(null, Instance.class));
        assertEquals("Query string cannot be null.", exception.getMessage());
    }

    /**
     * Test to remove a object from a non-existent collection
     */
    @Test
    public void testRemove_FromNonExistingCollection_1() {
        String jxQuery = String.format("/.[id='%s']", "12");
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.findAndRemove(jxQuery, Site.class));
        assertEquals("Collection by name 'sites' not found. Create collection first.", exception.getMessage());
    }

    /**
     * Test to remove a object from a non-existent collection
     */
    @Test
    public void testRemove_FromNonExistingCollection_2() {
        String jxQuery = String.format("/.[id='%s']", "12");
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.findAndRemove(jxQuery, "sites"));
        assertEquals("Collection by name 'sites' not found. Create collection first.", exception.getMessage());
    }

    /**
     * Test to remove a single object from a collection
     */
    @Test
    public void testRemove_ValidObject() {
        List<Instance> instances = jsonDBTemplate.getCollection(Instance.class);
        int size = instances.size();

        String jxQuery = String.format("/.[id='%s']", "05");
        Instance removedObject = jsonDBTemplate.findAndRemove(jxQuery, Instance.class);

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
    public void testRemove_OneofManyObjects() {
        List<Instance> instances = jsonDBTemplate.getCollection(Instance.class);
        int size = instances.size();

        String jxQuery = String.format("/.[id>'%s']", "04");

        Instance removedObjects = jsonDBTemplate.findAndRemove(jxQuery, Instance.class);

        instances = jsonDBTemplate.getCollection(Instance.class);
        assertNotNull(instances);
        assertEquals(size - 1, instances.size());
        assertNotNull(removedObjects);
    }

    /**
     * Test to remove a batch of objects from collection
     */
    @Test
    public void testRemove_BatchOfObjects() {
        List<Instance> instances = jsonDBTemplate.getCollection(Instance.class);
        int size = instances.size();

        String jxQuery = String.format("/.[id>'%s']", "04");

        List<Instance> removedObjects = jsonDBTemplate.findAllAndRemove(jxQuery, Instance.class);

        instances = jsonDBTemplate.getCollection(Instance.class);
        assertNotNull(instances);
        assertEquals(size - 2, instances.size());
        assertNotNull(removedObjects);
        assertEquals(2, removedObjects.size());
    }

    /**
     * Test to remove a object from a non-existent collection
     */
    @Test
    public void testFindAllAndRemove_FromNonExistingCollection_1() {
        String jxQuery = String.format("/.[id='%s']", "12");
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.findAllAndRemove(jxQuery, Site.class));
        assertEquals("Collection by name 'sites' not found. Create collection first.", exception.getMessage());
    }

    /**
     * Test to remove a object from a non-existent collection
     */
    @Test
    public void testFindAllAndRemove_FromNonExistingCollection_2() {
        String jxQuery = String.format("/.[id='%s']", "12");
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.findAllAndRemove(jxQuery, "sites"));
        assertEquals("Collection by name 'sites' not found. Create collection first.", exception.getMessage());
    }
}
