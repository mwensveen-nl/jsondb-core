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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import io.jsondb.JsonDBTemplate;
import io.jsondb.crypto.Default1Cipher;
import io.jsondb.crypto.ICipher;
import io.jsondb.tests.model.Instance;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests that cover all aspects of DB initialization
 *
 * @author Farooq Khan
 * @version 1.0 31 Dec 2015
 */
public class InitializeDBTests {
    private static final String INSTANCES_JSON = "instances.json";

    private ObjectMapper objectMapper = null;
    @TempDir
    private File dbFilesFolder;
    private ICipher cipher;

    @BeforeEach
    public void setup() throws IOException {
        dbFilesFolder.mkdir();
        Files.copy(new File("src/test/resources/dbfiles/instances.json"), new File(dbFilesFolder, INSTANCES_JSON));
        try {
            cipher = new Default1Cipher("1r8+24pibarAWgS85/Heeg==");
        } catch (GeneralSecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testInitialization() {
        JsonDBTemplate jsonDBTemplate = new JsonDBTemplate(dbFilesFolder.getAbsolutePath(), "io.jsondb.tests.model");

        Set<String> collectionNames = jsonDBTemplate.getCollectionNames();
        assertTrue(collectionNames.contains("instances"));
        assertEquals(collectionNames.size(), 1);
    }

    @Test
    public void testReload() {
        JsonDBTemplate jsonDBTemplate = new JsonDBTemplate(dbFilesFolder.getAbsolutePath(), "io.jsondb.tests.model", cipher);

        Set<String> collectionNames = jsonDBTemplate.getCollectionNames();
        assertTrue(collectionNames.contains("instances"));
        List<Instance> instances = jsonDBTemplate.findAll(Instance.class);
        int size = instances.size();

        // Add more computers directly to the computers.json file.
        List<Instance> instances1 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Instance inst = new Instance();
            int id = 11 + i;
            inst.setId(String.format("%02d", id));
            inst.setHostname("ec2-54-191-" + id);
            // Private key is encrypted form of: b87eb02f5dd7e5232d7b0fc30a5015e4
            inst.setPrivateKey("vr90J53rB/gXDb7XfALayqYXcVxHUT4eU+HqsTcpCI2rEmeeqwsHXEnpZxF4rzRCfDZs7NzSODRkPGgOHWmslQ==");
            inst.setPublicKey("d3aa045f71bf4d1dffd2c5f485a4bc1d");
            instances1.add(inst);
        }
        appendDirectlyToJsonFile(instances1, new File(dbFilesFolder, INSTANCES_JSON));

        jsonDBTemplate.reLoadDB();

        collectionNames = jsonDBTemplate.getCollectionNames();
        assertTrue(collectionNames.contains("instances"));
        instances = jsonDBTemplate.findAll(Instance.class);
        assertEquals(instances.size(), size + 10);
    }

    private <T> boolean appendDirectlyToJsonFile(List<T> collectionData, File collectionFile) {

        boolean retval = false;
        try (FileWriter fw = new FileWriter(collectionFile, true)) {
            ;
            for (T row : collectionData) {
                fw.write(objectMapper.writeValueAsString(row));
                fw.write("\n");
            }
            retval = true;
        } catch (IOException e) {
            retval = false;
            e.printStackTrace();
        }
        return retval;
    }
}
