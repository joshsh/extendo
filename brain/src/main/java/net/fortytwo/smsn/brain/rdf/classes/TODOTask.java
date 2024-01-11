package net.fortytwo.smsn.brain.rdf.classes;

import net.fortytwo.smsn.brain.model.entities.Note;
import net.fortytwo.smsn.brain.rdf.NoteClass;
import net.fortytwo.smsn.brain.rdf.RDFizationContext;
import net.fortytwo.smsn.rdf.vocab.SmSnVocabulary;
import org.openrdf.model.IRI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

import java.util.regex.Pattern;

public class TODOTask extends NoteClass {

    public TODOTask() {
        super(
                "todo",
                Pattern.compile("TODO: .+"),
                null,
                null
                );
    }

    @Override
    protected boolean isCollectionClass() {
        return false;
    }

    @Override
    public IRI toRDF(final Note a, RDFizationContext context) throws RDFHandlerException {
        ValueFactory vf = context.getValueFactory();
        RDFHandler handler = context.getHandler();

        IRI self = handleTypeAndAlias(a, context, SmSnVocabulary.TODO);

        // assumes the prefix "TODO:"
        String d = Note.getTitle(a).substring(5).trim();
        handler.handleStatement(vf.createStatement(self, RDFS.COMMENT, vf.createLiteral(d)));

        return self;
    }
}
