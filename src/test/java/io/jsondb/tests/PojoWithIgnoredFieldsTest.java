package io.jsondb.tests;

import com.google.common.io.Files;
import io.jsondb.JsonDBTemplate;
import io.jsondb.tests.model.PojoWithIgnoredFields;
import io.jsondb.tests.util.TestUtils;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test Class for a Pojo that uses the JsonIgnore or JsonIgnoreProperties annotation.
 *
 * @author Micha Wensveen
 */
public class PojoWithIgnoredFieldsTest {
    private static final String POJOWITHIGNOREDFIELDS_JSON = "pojowithignoredfields.json";
    @TempDir
    private File dbFilesFolder;
    private JsonDBTemplate jsonDBTemplate = null;

    @BeforeEach
    public void setUp() throws Exception {
        Files.copy(new File("src/test/resources/dbfiles/pojowithenumfields.json"), new File(dbFilesFolder, POJOWITHIGNOREDFIELDS_JSON));
        jsonDBTemplate = new JsonDBTemplate(dbFilesFolder.getAbsolutePath(), "io.jsondb.tests.model");
    }

    @Test
    public void testFind() {
        PojoWithIgnoredFields pojo = jsonDBTemplate.findById("0001", PojoWithIgnoredFields.class);

        assertNotNull(pojo);

        assertEquals(pojo.getStatus(), "CREATED");
        assertNull(pojo.getIgnored());
    }

    @Test
    public void testInsert() {
        List<PojoWithIgnoredFields> pojos = jsonDBTemplate.getCollection(PojoWithIgnoredFields.class);
        int size = pojos.size();

        PojoWithIgnoredFields pojo = new PojoWithIgnoredFields();
        pojo.setId("0010");
        pojo.setStatus("UPDATED");
        pojo.setIgnored("ignoredValue");
        jsonDBTemplate.insert(pojo);

        pojos = jsonDBTemplate.getCollection(PojoWithIgnoredFields.class);
        assertNotNull(pojos);
        assertEquals(pojos.size(), size + 1);
        String[] expectedLinesAtEnd = { "{\"id\":\"0010\",\"status\":\"UPDATED\"}" };
        TestUtils.checkLastLines(new File(dbFilesFolder, POJOWITHIGNOREDFIELDS_JSON), expectedLinesAtEnd);
    }
}
