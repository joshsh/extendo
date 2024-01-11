package net.fortytwo.smsn.brain.io.graphml;

import net.fortytwo.smsn.brain.Brain;
import net.fortytwo.smsn.brain.BrainTestBase;
import net.fortytwo.smsn.brain.io.NoteReader;
import net.fortytwo.smsn.brain.io.NoteWriter;
import net.fortytwo.smsn.brain.io.Format;
import net.fortytwo.smsn.brain.model.TopicGraph;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.assertNotNull;

public class GraphMLReaderIT extends BrainTestBase {

    private final File inputFile = new File("/tmp/smsn.xml");
    private final File outputFile = new File("/tmp/smsn-out.xml");

    @Override
    protected TopicGraph createTopicGraph() throws IOException {
        return createNeo4jTopicGraph();
    }

    @Test
    public void testTmp() throws Exception {
        Brain brain = new Brain(topicGraph);

        Format format = Format.getFormat("graphml");
        assertNotNull(format);
        NoteReader reader = Format.getReader(format);
        assertNotNull(reader);

        reader.doImport(inputFile, GraphMLFormat.getInstance(), brain);

        System.out.println("# notes: " + countNotes());

        NoteWriter exporter = new GraphMLWriter();
        try (OutputStream out = new FileOutputStream(outputFile)) {
            NoteWriter.Context context = new NoteWriter.Context();
            context.setTopicGraph(brain.getTopicGraph());
            context.setKnowledgeBase(brain.getKnowledgeBase());
            context.setDestStream(out);
            context.setFormat(GraphMLFormat.getInstance());
            exporter.doWrite(context);
        }
    }
}
