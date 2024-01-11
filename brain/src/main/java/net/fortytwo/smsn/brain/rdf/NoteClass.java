package net.fortytwo.smsn.brain.rdf;

import net.fortytwo.smsn.SemanticSynchrony;
import net.fortytwo.smsn.brain.model.entities.Note;
import net.fortytwo.smsn.brain.rdf.classes.AKAReference;
import net.fortytwo.smsn.rdf.vocab.FOAF;
import org.openrdf.model.IRI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandlerException;

import java.util.logging.Level;
import java.util.regex.Pattern;

public abstract class NoteClass {

    protected String name;
    protected Pattern valueRegex;
    protected final Pattern aliasRegex;
    protected final NoteReqex memberRegex;

    protected NoteClass(final String name,
                        final Pattern valueRegex,
                        final Pattern aliasRegex,
                        final NoteReqex memberRegex) {
        this.name = name;
        this.valueRegex = valueRegex;
        this.aliasRegex = aliasRegex;
        this.memberRegex = memberRegex;
    }

    public String getName() {
        return name;
    }

    public Pattern getValueRegex() {
        return valueRegex;
    }

    public Pattern getAliasRegex() {
        return aliasRegex;
    }

    protected abstract boolean isCollectionClass();

    public abstract IRI toRDF(Note a, RDFizationContext context) throws RDFHandlerException;

    protected IRI handleTypeAndAlias(final Note a,
                                     final RDFizationContext context,
                                     final IRI type) throws RDFHandlerException {
        IRI self = context.getValueFactory().createIRI(context.getTopicGraph().iriOf(a));

        context.getHandler().handleStatement(context.getValueFactory().createStatement(self, RDF.TYPE, type));

        if (null != Note.getAlias(a)) {
            IRI aliasIRI;

            try {
                aliasIRI = context.getValueFactory().createIRI(Note.getAlias(a));
            } catch (Exception e) {
                SemanticSynchrony.getLogger().log(Level.WARNING, "alias is not a valid IRI: " + Note.getAlias(a), e);
                aliasIRI = null;
            }

            if (null != aliasIRI) {
                context.getHandler().handleStatement(
                        context.getValueFactory().createStatement(self, OWL.SAMEAS,
                                context.getValueFactory().createIRI(Note.getAlias(a))));
            }
        }

        return self;
    }

    public interface FieldHandler {
        void handle(Note object, RDFizationContext context) throws RDFHandlerException;
    }

    public static class NickHandler implements FieldHandler {
        @Override
        public void handle(Note object, RDFizationContext context) throws RDFHandlerException {
            ValueFactory vf = context.getValueFactory();

            // TODO: this is an abuse of foaf:nick even when the domain is foaf:Person as it is here...
            // foaf:nick is supposed to be used for online handles, not aliases in general
            context.getHandler().handleStatement(
                    vf.createStatement(
                            context.getSubjectIri(), FOAF.NICK, vf.createLiteral(
                                    AKAReference.extractAlias(Note.getTitle(object)))));
        }
    }

    public static class PageHandler implements FieldHandler {
        @Override
        public void handle(Note object, RDFizationContext context) throws RDFHandlerException {
            ValueFactory vf = context.getValueFactory();
            IRI objectIRI = context.iriOf(object);
            context.getHandler().handleStatement(vf.createStatement(
                    // note: use of foaf:page rather than foaf:homepage avoids the assumption that the link is
                    // always a home page, although this is frequently the case
                    context.getSubjectIri(), FOAF.PAGE, objectIRI));
        }
    }

    public static class RFIDHandler implements FieldHandler {
        @Override
        public void handle(Note object, RDFizationContext context) throws RDFHandlerException {
            // TODO

            // note: there are no relevant properties in the BTC 2014 matching "rfid":
            // grep -i rfid btc-predicates-frequency.tsv
            // 1       http://dbpedia-live.openlinksw.com/property/perfid
        }
    }

    public static class DocumentsAboutTopicHandler implements FieldHandler {
        @Override
        public void handle(Note object, RDFizationContext context) throws RDFHandlerException {
            ValueFactory vf = context.getValueFactory();
            IRI objectIRI = context.iriOf(object);
            context.getHandler().handleStatement(vf.createStatement(
                    objectIRI, FOAF.TOPIC, context.getSubjectIri()));
            // The skos:note on dc:subject reads "This term is intended to be used with non-literal values
            // as defined in the DCMI Abstract Model (http://dublincore.org/documents/abstract-model/) [...]"
            //objectIRI, DCTerms.SUBJECT, context.getSubjectIri()));
        }
    }
}
