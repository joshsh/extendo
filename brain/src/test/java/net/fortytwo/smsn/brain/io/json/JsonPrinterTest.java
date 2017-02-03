package net.fortytwo.smsn.brain.io.json;

import net.fortytwo.smsn.SemanticSynchrony;
import net.fortytwo.smsn.brain.io.wiki.WikiParser;
import net.fortytwo.smsn.brain.model.Note;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JsonPrinterTest {
    private WikiParser wikiParser;
    private JsonPrinter jsonPrinter;

    @Before
    public void setUp() {
        wikiParser = new WikiParser();
        jsonPrinter = new JsonPrinter();
    }

    @Test
    public void jsonOutputIsNormal() throws Exception {
        Note note = wikiParser.parse("" +
                "* foo\n" +
                "   * bar\n" +
                "   * quux\n");

        JSONObject j = jsonPrinter.toJson(note);

        assertEquals(0, j.getInt(JsonFormat.NUMBER_OF_CHILDREN));
        assertEquals(0, j.getInt(JsonFormat.NUMBER_OF_PARENTS));
        JSONArray c = j.getJSONArray(JsonFormat.CHILDREN);
        assertEquals(1, c.length());

        JSONObject n1 = c.getJSONObject(0);
        assertEquals(0, n1.getInt(JsonFormat.NUMBER_OF_CHILDREN));
        assertEquals(0, n1.getInt(JsonFormat.NUMBER_OF_PARENTS));
        assertEquals("foo", n1.getString(SemanticSynchrony.TITLE));
        JSONArray c1 = n1.getJSONArray(JsonFormat.CHILDREN);
        assertEquals(2, c1.length());

        JSONObject n2 = c1.getJSONObject(0);
        assertEquals(0, n2.getInt(JsonFormat.NUMBER_OF_CHILDREN));
        assertEquals(0, n2.getInt(JsonFormat.NUMBER_OF_PARENTS));
        assertEquals("bar", n2.getString(SemanticSynchrony.TITLE));
        assertNull(n2.optJSONArray(JsonFormat.CHILDREN));

        JSONObject n3 = c1.getJSONObject(1);
        assertEquals(0, n3.getInt(JsonFormat.NUMBER_OF_CHILDREN));
        assertEquals(0, n3.getInt(JsonFormat.NUMBER_OF_PARENTS));
        assertEquals("quux", n3.getString(SemanticSynchrony.TITLE));
        assertNull(n3.optJSONArray(JsonFormat.CHILDREN));
    }

    @Test
    public void longValuesAreTruncated() throws Exception {
        Note n = wikiParser.parse("" +
                "* this is a long line (well, not really)\n");

        int before = jsonPrinter.getTitleLengthCutoff();
        try {
            jsonPrinter.setTitleLengthCutoff(10);

            JSONObject j = jsonPrinter.toJson(n);

            assertEquals("this is a  [...]",
                    j.getJSONArray(JsonFormat.CHILDREN).getJSONObject(0).getString(SemanticSynchrony.TITLE));
        } finally {
            jsonPrinter.setTitleLengthCutoff(before);
        }
    }

    @Test
    public void noPageGivesNoPageAttribute() throws Exception {
        Note n = new Note();
        n.setTitle("Arthur Dent");

        JSONObject j = jsonPrinter.toJson(n);
        assertEquals("Arthur Dent", j.getString(SemanticSynchrony.TITLE));
        assertNull(j.opt(SemanticSynchrony.PAGE));
    }

    @Test
    public void pageGivesPageAttribute() throws Exception {
        Note n = new Note();
        n.setTitle("Arthur Dent");
        n.setPage("12345");

        JSONObject j = jsonPrinter.toJson(n);
        assertEquals("Arthur Dent", j.getString(SemanticSynchrony.TITLE));
        assertEquals("12345", j.getString(SemanticSynchrony.PAGE));
    }
}
