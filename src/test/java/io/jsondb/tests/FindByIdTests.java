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
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @version 1.0 14-Oct-2016
 */
public class FindByIdTests {
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
     * test to find a document with a existing id.
     */
    @Test
    public void testFindById_ForExistingId() {
        Instance instance = jsonDBTemplate.findById("01", Instance.class);
        assertNotNull(instance);
        assertEquals(instance.getId(), "01");
    }

    /**
     * test to find a document with a non-existent id.
     */
    @Test
    public void testFindById_ForNonExistentId() {
        Instance instance = jsonDBTemplate.findById("00", Instance.class);
        assertNull(instance);
    }

    private class NonAnotatedClass {
        // intentionately empty
    }

    /**
     * test to find a document formEntity type which does not have @Document annotation
     */
    @Test
    public void testFindById_NonAnotatedClass() {
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.findById("000000", NonAnotatedClass.class));
        assertEquals("Entity 'NonAnotatedClass' is not annotated with annotation @Document", exception.getMessage());
    }

    /**
     * test to find a document for a unknown collection name
     */
    @Test
    public void testFindById_UnknownCollectionName() {
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.findById("000000", "SomeCollection"));
        assertEquals("Collection by name 'SomeCollection' not found. Create collection first.", exception.getMessage());
    }
}
