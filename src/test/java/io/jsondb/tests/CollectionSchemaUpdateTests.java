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
import io.jsondb.query.ddl.AddOperation;
import io.jsondb.query.ddl.CollectionSchemaUpdate;
import io.jsondb.query.ddl.DeleteOperation;
import io.jsondb.query.ddl.IOperation;
import io.jsondb.query.ddl.RenameOperation;
import io.jsondb.tests.model.LoadBalancer;
import io.jsondb.tests.util.TestUtils;
import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @version 1.0 25-Oct-2016
 */
public class CollectionSchemaUpdateTests {
    private static final String LOADBALANCER_JSON = "loadbalancer.json";

    @TempDir
    private File dbFilesFolder;

    private JsonDBTemplate jsonDBTemplate = null;

    @BeforeEach
    public void setUp() throws Exception {
        Files.copy(new File("src/test/resources/dbfiles/loadbalancer.json"), new File(dbFilesFolder, LOADBALANCER_JSON));
        jsonDBTemplate = new JsonDBTemplate(dbFilesFolder.getAbsolutePath(), "io.jsondb.tests.model", null, true, null);
    }

    @Test
    public void test_RenameField() {
        assertTrue(jsonDBTemplate.isCollectionReadonly(LoadBalancer.class));

        IOperation renOperation = new RenameOperation("admin");
        CollectionSchemaUpdate cu = CollectionSchemaUpdate.update("username", renOperation);

        Map<String, IOperation> allUpdateOps = cu.getUpdateData();
        assertEquals(1, allUpdateOps.size());

        jsonDBTemplate.updateCollectionSchema(cu, LoadBalancer.class);

        assertFalse(jsonDBTemplate.isCollectionReadonly(LoadBalancer.class));

        String[] expectedLinesAtEnd = {
                "{\"schemaVersion\":\"1.0\"}",
                "{\"id\":\"001\",\"hostname\":\"eclb-54-01\",\"admin\":\"admin\",\"osName\":null}",
                "{\"id\":\"002\",\"hostname\":\"eclb-54-02\",\"admin\":\"admin\",\"osName\":null}",
                "{\"id\":\"003\",\"hostname\":\"eclb-54-03\",\"admin\":\"admin\",\"osName\":null}",
                "{\"id\":\"004\",\"hostname\":\"eclb-54-04\",\"admin\":\"admin\",\"osName\":null}",
                "{\"id\":\"005\",\"hostname\":\"eclb-54-05\",\"admin\":\"admin\",\"osName\":null}",
                "{\"id\":\"006\",\"hostname\":\"eclb-54-06\",\"admin\":\"admin\",\"osName\":null}",
                "{\"id\":\"007\",\"hostname\":\"eclb-54-07\",\"admin\":\"admin\",\"osName\":null}",
                "{\"id\":\"008\",\"hostname\":\"eclb-54-08\",\"admin\":\"admin\",\"osName\":null}",
                "{\"id\":\"009\",\"hostname\":\"eclb-54-09\",\"admin\":\"admin\",\"osName\":null}",
                "{\"id\":\"010\",\"hostname\":\"eclb-54-10\",\"admin\":\"admin\",\"osName\":null}" };

        TestUtils.checkLastLines(new File(dbFilesFolder, LOADBALANCER_JSON), expectedLinesAtEnd);
    }

    @Test
    public void test_AddDeleteField() {
        assertTrue(jsonDBTemplate.isCollectionReadonly(LoadBalancer.class));

        IOperation addOperation = new AddOperation("mac", false);
        CollectionSchemaUpdate cu = CollectionSchemaUpdate.update("osName", addOperation);

        jsonDBTemplate.updateCollectionSchema(cu, LoadBalancer.class);

        assertFalse(jsonDBTemplate.isCollectionReadonly(LoadBalancer.class));

        String[] expectedLinesAtEnd = {
                "{\"schemaVersion\":\"1.0\"}",
                "{\"id\":\"001\",\"hostname\":\"eclb-54-01\",\"username\":\"admin\",\"osName\":\"mac\"}",
                "{\"id\":\"002\",\"hostname\":\"eclb-54-02\",\"username\":\"admin\",\"osName\":\"mac\"}",
                "{\"id\":\"003\",\"hostname\":\"eclb-54-03\",\"username\":\"admin\",\"osName\":\"mac\"}",
                "{\"id\":\"004\",\"hostname\":\"eclb-54-04\",\"username\":\"admin\",\"osName\":\"mac\"}",
                "{\"id\":\"005\",\"hostname\":\"eclb-54-05\",\"username\":\"admin\",\"osName\":\"mac\"}",
                "{\"id\":\"006\",\"hostname\":\"eclb-54-06\",\"username\":\"admin\",\"osName\":\"mac\"}",
                "{\"id\":\"007\",\"hostname\":\"eclb-54-07\",\"username\":\"admin\",\"osName\":\"mac\"}",
                "{\"id\":\"008\",\"hostname\":\"eclb-54-08\",\"username\":\"admin\",\"osName\":\"mac\"}",
                "{\"id\":\"009\",\"hostname\":\"eclb-54-09\",\"username\":\"admin\",\"osName\":\"mac\"}",
                "{\"id\":\"010\",\"hostname\":\"eclb-54-10\",\"username\":\"admin\",\"osName\":\"mac\"}" };

        TestUtils.checkLastLines(new File(dbFilesFolder, LOADBALANCER_JSON), expectedLinesAtEnd);
    }

    @Test
    public void test_OnlyDeleteField() {
        assertTrue(jsonDBTemplate.isCollectionReadonly("loadbalancer"));

        IOperation delOperation = new DeleteOperation();
        CollectionSchemaUpdate cu = CollectionSchemaUpdate.update("deletedField", delOperation);

        jsonDBTemplate.updateCollectionSchema(cu, "loadbalancer");

        assertFalse(jsonDBTemplate.isCollectionReadonly("loadbalancer"));

        String[] expectedLinesAtEnd = {
                "{\"schemaVersion\":\"1.0\"}",
                "{\"id\":\"001\",\"hostname\":\"eclb-54-01\",\"username\":\"admin\",\"osName\":null}",
                "{\"id\":\"002\",\"hostname\":\"eclb-54-02\",\"username\":\"admin\",\"osName\":null}",
                "{\"id\":\"003\",\"hostname\":\"eclb-54-03\",\"username\":\"admin\",\"osName\":null}",
                "{\"id\":\"004\",\"hostname\":\"eclb-54-04\",\"username\":\"admin\",\"osName\":null}",
                "{\"id\":\"005\",\"hostname\":\"eclb-54-05\",\"username\":\"admin\",\"osName\":null}",
                "{\"id\":\"006\",\"hostname\":\"eclb-54-06\",\"username\":\"admin\",\"osName\":null}",
                "{\"id\":\"007\",\"hostname\":\"eclb-54-07\",\"username\":\"admin\",\"osName\":null}",
                "{\"id\":\"008\",\"hostname\":\"eclb-54-08\",\"username\":\"admin\",\"osName\":null}",
                "{\"id\":\"009\",\"hostname\":\"eclb-54-09\",\"username\":\"admin\",\"osName\":null}",
                "{\"id\":\"010\",\"hostname\":\"eclb-54-10\",\"username\":\"admin\",\"osName\":null}" };

        TestUtils.checkLastLines(new File(dbFilesFolder, LOADBALANCER_JSON), expectedLinesAtEnd);
    }

    @Test
    public void test_RenameInNonExistingCollection() {
        IOperation renOperation = new RenameOperation("admin");
        CollectionSchemaUpdate cu = CollectionSchemaUpdate.update("username", renOperation);

        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.updateCollectionSchema(cu, "sites"));
        assertEquals("Collection by name 'sites' not found. Create collection first.", exception.getMessage());
    }

    @Test
    public void test_AddToNonExistingCollection() {
        IOperation addOperation = new AddOperation("mac", false);
        CollectionSchemaUpdate cu = CollectionSchemaUpdate.update("osName", addOperation);

        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.updateCollectionSchema(cu, "sites"));
        assertEquals("Collection by name 'sites' not found. Create collection first.", exception.getMessage());
    }

    @Test
    public void test_DeleteFromNonExistingCollection() {
        IOperation delOperation = new DeleteOperation();
        CollectionSchemaUpdate cu = CollectionSchemaUpdate.update("deletedField", delOperation);

        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.updateCollectionSchema(cu, "sites"));
        assertEquals("Collection by name 'sites' not found. Create collection first.", exception.getMessage());
    }
}
