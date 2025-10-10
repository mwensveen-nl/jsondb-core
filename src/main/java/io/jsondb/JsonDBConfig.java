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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.jsondb.crypto.ICipher;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Comparator;
import lombok.Getter;
import lombok.Setter;

/**
 * A POJO that has settings for the functioning of DB.
 * 
 * @author Farooq Khan
 * @version 1.0 25-Sep-2016
 */
public class JsonDBConfig {
    // Settings
    @Getter
    @Setter
    private Charset charset;
    @Getter
    private String dbFilesLocationString;
    @Getter
    private File dbFilesLocation;
    @Getter
    private Path dbFilesPath;
    @Getter
    @Setter
    private String baseScanPackage;
    @Getter
    @Setter
    private ICipher cipher;
    @Getter
    private boolean compatibilityMode;

    // References
    @Getter
    @Setter
    private ObjectMapper objectMapper;
    @Getter
    private Comparator<String> schemaComparator;

    public JsonDBConfig(String dbFilesLocationString, String baseScanPackage,
            ICipher cipher, boolean compatibilityMode, Comparator<String> schemaComparator) {

        this.charset = Charset.forName("UTF-8");
        this.dbFilesLocationString = dbFilesLocationString;
        this.dbFilesLocation = new File(dbFilesLocationString);
        this.dbFilesPath = dbFilesLocation.toPath();
        this.baseScanPackage = baseScanPackage;
        this.cipher = cipher;

        this.compatibilityMode = compatibilityMode;
        this.objectMapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());

        if (compatibilityMode) {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        if (null == schemaComparator) {
            this.schemaComparator = new DefaultSchemaVersionComparator();
        } else {
            this.schemaComparator = schemaComparator;
        }
    }

    public void setDbFilesLocationString(String dbFilesLocationString) {
        this.dbFilesLocationString = dbFilesLocationString;
        this.dbFilesLocation = new File(dbFilesLocationString);
        this.dbFilesPath = dbFilesLocation.toPath();
    }

    public void setCompatibilityMode(boolean compatibilityMode) {
        this.compatibilityMode = compatibilityMode;
        if (compatibilityMode) {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        } else {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        }
    }

}
