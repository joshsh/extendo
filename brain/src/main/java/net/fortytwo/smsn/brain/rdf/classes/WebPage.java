package net.fortytwo.smsn.brain.rdf.classes;

import net.fortytwo.smsn.brain.model.entities.Note;
import net.fortytwo.smsn.brain.rdf.NoteClass;
import net.fortytwo.smsn.brain.rdf.RDFizationContext;
import net.fortytwo.smsn.rdf.vocab.FOAF;
import org.openrdf.model.IRI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

import java.util.regex.Pattern;

public class WebPage extends NoteClass {

    public WebPage() {
        super(
                "webpage",
                Pattern.compile(".+ \\(web page\\)"),
                Pattern.compile("http(s)?://.+"),
                null
                );
    }

    @Override
    protected boolean isCollectionClass() {
        return false;
    }

    @Override
    public IRI toRDF(Note a, RDFizationContext context) throws RDFHandlerException {
        ValueFactory vf = context.getValueFactory();
        RDFHandler handler = context.getHandler();

        IRI self = handleTypeAndAlias(a, context, FOAF.DOCUMENT);

        // note: assumes that the value ends with "(web page)"
        int i = Note.getTitle(a).lastIndexOf("(");
        String d = Note.getTitle(a).substring(0, i).trim();
        handler.handleStatement(vf.createStatement(self, DCTERMS.TITLE, vf.createLiteral(d)));

        return self;
    }
}
