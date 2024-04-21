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
import io.jsondb.JsonDBTemplate;
import io.jsondb.crypto.Default1Cipher;
import io.jsondb.crypto.ICipher;
import io.jsondb.events.CollectionFileChangeAdapter;
import io.jsondb.tests.model.Instance;
import io.jsondb.tests.model.PojoWithEnumFields;
import io.jsondb.tests.util.TestUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * @version 1.0 24-Oct-2016
 */
public class FileChangeAdapterTests {

    private static final String POJOWITHENUMFIELDS_JSON = "pojowithenumfields.json";
    private static final String INSTANCES_JSON = "instances.json";
    private static final long DB_RELOAD_TIMEOUT = 5 * 1000;
    @TempDir
    private File dbFilesFolder;

    private JsonDBTemplate jsonDBTemplate = null;

    @BeforeEach
    public void setUp() throws Exception {
        // Filewatcher does not work on Mac and hence JsonDB events will never fire
        // and so the EventTests will never succeed. So we run the tests only if
        // it is not a Mac system
        assumeFalse(TestUtils.isMac());

        Files.copy(new File("src/test/resources/dbfiles/instances.json"), new File(dbFilesFolder, INSTANCES_JSON));

        Files.copy(new File("src/test/resources/dbfiles/pojowithenumfields.json"), new File(dbFilesFolder, POJOWITHENUMFIELDS_JSON));

        ICipher cipher = new Default1Cipher("1r8+24pibarAWgS85/Heeg==");
        jsonDBTemplate = new JsonDBTemplate(dbFilesFolder.getAbsolutePath(), "io.jsondb.tests.model", cipher);
    }

    private boolean collectionFileAddedFired = false;

    private class FileAddedChangeAdapter extends CollectionFileChangeAdapter {
        private JsonDBTemplate myJonDBTemplate;

        public FileAddedChangeAdapter(JsonDBTemplate newJsonDBTemplate) {
            this.myJonDBTemplate = newJsonDBTemplate;
        }

        @Override
        public void collectionFileAdded(String collectionName) {
            super.collectionFileAdded(collectionName);
            myJonDBTemplate.reloadCollection(collectionName);
            collectionFileAddedFired = true;
        }
    }

    @Test
    public void testAutoReloadOnCollectionFileAdded() throws GeneralSecurityException, IOException {
        boolean deleted = new File(dbFilesFolder, INSTANCES_JSON).delete();
        assertTrue(deleted);

        ICipher cipher = new Default1Cipher("1r8+24pibarAWgS85/Heeg==");
        JsonDBTemplate newJsonDBTemplate = new JsonDBTemplate(dbFilesFolder.getAbsolutePath(), "io.jsondb.tests.model", cipher);

        newJsonDBTemplate.addCollectionFileChangeListener(new FileAddedChangeAdapter(newJsonDBTemplate));
        assertFalse(newJsonDBTemplate.collectionExists(Instance.class));

        File instancesJson = new File(dbFilesFolder, INSTANCES_JSON);
        Files.copy(new File("src/test/resources/dbfiles/instances.json"), instancesJson);
        sleep();

        assertTrue(collectionFileAddedFired);
        List<Instance> instances = newJsonDBTemplate.findAll(Instance.class);
        assertNotNull(instances);
        assertNotEquals(0, instances.size());
    }

    private boolean collectionFileModifiedFired = false;

    private class FileModifiedChangeAdapter extends CollectionFileChangeAdapter {
        @Override
        public void collectionFileModified(String collectionName) {
            super.collectionFileModified(collectionName);
            jsonDBTemplate.reloadCollection(collectionName);
            collectionFileModifiedFired = true;
        }
    }

    @Test
    public void testAutoReloadOnCollectionFileModified() throws IOException {
        int oldCount = jsonDBTemplate.findAll(Instance.class).size();
        jsonDBTemplate.addCollectionFileChangeListener(new FileModifiedChangeAdapter());
        assertFalse(collectionFileModifiedFired);

        @SuppressWarnings("resource")
        Scanner sc = new Scanner(new File("src/test/resources/dbfiles/instances.json")).useDelimiter("\\Z");
        String content = sc.next();
        sc.close();
        assertEquals(1131, content.length());
        content = content + "\n" + "{\"id\":\"07\",\"hostname\":\"ec2-54-191-07\","
                + "\"privateKey\":\"vr90J53rB/gXDb7XfALayqYXcVxHUT4eU+HqsTcpCI2rEmeeqwsHXEnpZxF4rzRCfDZs7NzSODRkPGgOHWmslQ==\","
                + "\"publicKey\":\"d3aa045f71bf4d1dffd2c5f485a4bc1d\"}";

        Files.write(content.getBytes(), new File(dbFilesFolder, INSTANCES_JSON));
        sleep();
        assertTrue(collectionFileModifiedFired);

        for (int i = 0; i < 5; i++) {
            int tempCount = jsonDBTemplate.findAll(Instance.class).size();
            if (tempCount == oldCount + 1) {
                break;
            }
            sleep();
        }

        int newCount = jsonDBTemplate.findAll(Instance.class).size();
        assertEquals(oldCount + 1, newCount);
    }

    private void sleep() {
        try {
            // Give it some time to reload DB
            Thread.sleep(DB_RELOAD_TIMEOUT);
        } catch (InterruptedException e) {
            fail("Failed to wait for db reload");
        }
    }

    private boolean collectionFileDeletedFired = false;

    private class FileDeletedChangeAdapter extends CollectionFileChangeAdapter {
        @Override
        public void collectionFileDeleted(String collectionName) {
            super.collectionFileDeleted(collectionName);
            jsonDBTemplate.reLoadDB();
            collectionFileDeletedFired = true;
        }
    }

    @Test
    public void testAutoReloadOnCollectionFileDeleted() throws FileNotFoundException {
        assertTrue(jsonDBTemplate.collectionExists(PojoWithEnumFields.class));

        jsonDBTemplate.addCollectionFileChangeListener(new FileDeletedChangeAdapter());

        new File(dbFilesFolder, POJOWITHENUMFIELDS_JSON).delete();

        sleep();

        assertTrue(collectionFileDeletedFired);
        assertFalse(jsonDBTemplate.collectionExists(PojoWithEnumFields.class));
    }

    private class DoNothingChangeAdapter extends CollectionFileChangeAdapter {
        // nothing to do
    }

    @Test
    public void testRemoveListener() {
        assertFalse(jsonDBTemplate.hasCollectionFileChangeListener());

        CollectionFileChangeAdapter adapter = new DoNothingChangeAdapter();
        jsonDBTemplate.addCollectionFileChangeListener(adapter);
        assertTrue(jsonDBTemplate.hasCollectionFileChangeListener());

        jsonDBTemplate.removeCollectionFileChangeListener(adapter);
        assertFalse(jsonDBTemplate.hasCollectionFileChangeListener());
    }
}
