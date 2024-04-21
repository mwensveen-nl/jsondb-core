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
import io.jsondb.events.CollectionFileChangeListener;
import io.jsondb.tests.model.Instance;
import io.jsondb.tests.model.PojoWithEnumFields;
import io.jsondb.tests.util.TestUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

/**
 * @version 1.0 24-Oct-2016
 */
public class FileChangeListenerTests {

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
        assertFalse(TestUtils.isMac());

        Files.copy(new File("src/test/resources/dbfiles/pojowithenumfields.json"), new File(dbFilesFolder, POJOWITHENUMFIELDS_JSON));
        ICipher cipher = new Default1Cipher("1r8+24pibarAWgS85/Heeg==");

        jsonDBTemplate = new JsonDBTemplate(dbFilesFolder.getAbsolutePath(), "io.jsondb.tests.model", cipher);
    }

    @Test
    public void testAutoReloadOnCollectionFileAdded() {
        jsonDBTemplate.addCollectionFileChangeListener(new CollectionFileChangeListener() {

            @Override
            public void collectionFileModified(String collectionName) {
                // intentionally left empty
            }

            @Override
            public void collectionFileDeleted(String collectionName) {
                // intentionally left empty
            }

            @Override
            public void collectionFileAdded(String collectionName) {
                jsonDBTemplate.reloadCollection(collectionName);
            }
        });

        // Add a additional do nothing listener to test addition of one more listener
        jsonDBTemplate.addCollectionFileChangeListener(new CollectionFileChangeListener() {
            @Override
            public void collectionFileModified(String collectionName) {
                // intentionally left empty
            }

            @Override
            public void collectionFileDeleted(String collectionName) {
                // intentionally left empty
            }

            @Override
            public void collectionFileAdded(String collectionName) {
                // intentionally left empty
            }
        });

        assertFalse(jsonDBTemplate.collectionExists(Instance.class));
        try {
            Files.copy(new File("src/test/resources/dbfiles/instances.json"), new File(dbFilesFolder, INSTANCES_JSON));
        } catch (IOException e1) {
            fail("Failed to copy data store files");
        }

        sleep();

        List<Instance> instances = jsonDBTemplate.findAll(Instance.class);
        assertNotNull(instances);
        assertNotEquals(instances.size(), 0);
    }

    @Test
    public void testAutoReloadOnCollectionFileModified() throws IOException {
        try {
            Files.copy(new File("src/test/resources/dbfiles/instances.json"), new File(dbFilesFolder, INSTANCES_JSON));
        } catch (IOException e1) {
            fail("Failed to copy data store files");
        }
        jsonDBTemplate.reLoadDB();
        int oldCount = jsonDBTemplate.findAll(Instance.class).size();
        jsonDBTemplate.addCollectionFileChangeListener(new CollectionFileChangeListener() {

            @Override
            public void collectionFileModified(String collectionName) {
                jsonDBTemplate.reloadCollection(collectionName);
            }

            @Override
            public void collectionFileDeleted(String collectionName) {
                // intentionally left empty
            }

            @Override
            public void collectionFileAdded(String collectionName) {
                // intentionally left empty
            }
        });

        @SuppressWarnings("resource")
        Scanner sc = new Scanner(new File("src/test/resources/dbfiles/instances.json")).useDelimiter("\\Z");
        String content = sc.next();
        sc.close();

        content = content + "\n" + "{\"id\":\"07\",\"hostname\":\"ec2-54-191-07\","
                + "\"privateKey\":\"vr90J53rB/gXDb7XfALayqYXcVxHUT4eU+HqsTcpCI2rEmeeqwsHXEnpZxF4rzRCfDZs7NzSODRkPGgOHWmslQ==\","
                + "\"publicKey\":\"d3aa045f71bf4d1dffd2c5f485a4bc1d\"}";

        Files.write(content.getBytes(), new File(dbFilesFolder, INSTANCES_JSON));

        sleep();
        for (int i = 0; i < 5; i++) {
            if (jsonDBTemplate.findAll(Instance.class).size() == oldCount + 1) {
                break;
            }
            sleep();
        }

        int newCount = jsonDBTemplate.findAll(Instance.class).size();
        assertEquals(oldCount + 1, newCount);
    }

    @Test
    public void testAutoReloadOnCollectionFileDeleted() throws FileNotFoundException {
        assertTrue(jsonDBTemplate.collectionExists(PojoWithEnumFields.class));

        jsonDBTemplate.addCollectionFileChangeListener(new CollectionFileChangeListener() {

            @Override
            public void collectionFileModified(String collectionName) {
                // intentionally left empty
            }

            @Override
            public void collectionFileDeleted(String collectionName) {
                jsonDBTemplate.reLoadDB();
            }

            @Override
            public void collectionFileAdded(String collectionName) {
                // intentionally left empty

            }
        });

        new File(dbFilesFolder, POJOWITHENUMFIELDS_JSON).delete();

        sleep();

        assertFalse(jsonDBTemplate.collectionExists(PojoWithEnumFields.class));
    }

    private void sleep() {
        try {
            // Give it some time to reload DB
            Thread.sleep(DB_RELOAD_TIMEOUT);
        } catch (InterruptedException e) {
            fail("Failed to wait for db reload");
        }
    }

    @Test
    public void testRemoveListener() {
        assertFalse(jsonDBTemplate.hasCollectionFileChangeListener());
        CollectionFileChangeListener listener = new CollectionFileChangeListener() {
            @Override
            public void collectionFileModified(String collectionName) {
                // intentionally left empty
            }

            @Override
            public void collectionFileDeleted(String collectionName) {
                // intentionally left empty
            }

            @Override
            public void collectionFileAdded(String collectionName) {
                // intentionally left empty
            }
        };

        jsonDBTemplate.addCollectionFileChangeListener(listener);
        assertTrue(jsonDBTemplate.hasCollectionFileChangeListener());

        jsonDBTemplate.removeCollectionFileChangeListener(listener);
        assertFalse(jsonDBTemplate.hasCollectionFileChangeListener());
    }
}
