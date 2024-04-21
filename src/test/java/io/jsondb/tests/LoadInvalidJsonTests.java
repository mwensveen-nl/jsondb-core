/*
 * Copyright (c) 2018 Farooq Khan
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
import io.jsondb.tests.util.TestUtils;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @version 1.0 19-Oct-2018
 */
public class LoadInvalidJsonTests {
    private static final String INSTANCES_JSON = "instances.json";
    @TempDir
    private File dbFilesFolder;

    @BeforeEach
    public void setup() throws IOException {
        Files.copy(new File("src/test/resources/dbfiles/instances.json"), new File(dbFilesFolder.getAbsolutePath(), INSTANCES_JSON));
    }

    /**
     * A test to ensure JsonDB does not delete the source files when a exception occurs during loading
     */
    @Test
    public void testLoadForInvalidJson() {
        // The invalidJson has semicolon instead of a colon between the id attribute name and value
        String invalidJson = "{\"id\"=\"07\",\"hostname\":\"ec2-54-191-07\",\"privateKey\":\"Zf9vl5K6WV6BA3eL7JbnrfPMjfJxc9Rkoo0zlROQlgTslmcp9iFzos+MP93GZqop\",\"publicKey\":\"\"}";

        TestUtils.appendDirectToFile(new File(dbFilesFolder, INSTANCES_JSON), invalidJson);

        JsonDBTemplate jsonDBTemplate = new JsonDBTemplate(dbFilesFolder.getAbsolutePath(), "io.jsondb.tests.model");
        Set<String> collectionNames = jsonDBTemplate.getCollectionNames();

        assertEquals(collectionNames.size(), 0);
        assertEquals(8, TestUtils.getNoOfLinesInFile(new File(dbFilesFolder, INSTANCES_JSON)));
    }
}
