package net.fortytwo.smsn.server;

import com.google.common.base.Preconditions;
import net.fortytwo.smsn.SemanticSynchrony;
import net.fortytwo.smsn.brain.AtomId;
import net.fortytwo.smsn.brain.Brain;
import net.fortytwo.smsn.brain.History;
import net.fortytwo.smsn.brain.Params;
import net.fortytwo.smsn.brain.io.json.JsonParser;
import net.fortytwo.smsn.brain.io.json.JsonPrinter;
import net.fortytwo.smsn.brain.io.wiki.WikiParser;
import net.fortytwo.smsn.brain.model.Filter;
import net.fortytwo.smsn.brain.model.TopicGraph;
import net.fortytwo.smsn.brain.model.entities.Note;
import net.fortytwo.smsn.brain.model.entities.Link;
import net.fortytwo.smsn.brain.model.entities.TreeNode;
import net.fortytwo.smsn.brain.model.pg.GraphWrapper;
import net.fortytwo.smsn.brain.model.pg.neo4j.Neo4jGraphWrapper;
import net.fortytwo.smsn.brain.model.pg.PGTopicGraph;
import net.fortytwo.smsn.brain.model.pg.tg.TinkerGraphWrapper;
import net.fortytwo.smsn.brain.query.TreeViews;
import net.fortytwo.smsn.server.errors.BadRequestException;
import net.fortytwo.smsn.server.errors.RequestProcessingException;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.shaded.jackson.annotation.JsonIgnoreProperties;
import org.apache.tinkerpop.shaded.jackson.annotation.JsonTypeInfo;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "action")
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Action {

    protected static final Logger logger = Logger.getLogger(Action.class.getName());

    protected static final int MAX_VIEW_HEIGHT = 7;

    protected static final AtomId CREATE_NEW_NOTE = new AtomId("create-new-note");

    private static final Map<Graph, Brain> brains = new HashMap<>();
    private static final Map<Graph, GraphWrapper> wrappers = new HashMap<>();

    private static final History history = new History();

    // override in subclasses
    protected void performTransaction(final ActionContext context)
            throws BadRequestException, RequestProcessingException {}

    protected abstract boolean doesRead();

    protected abstract boolean doesWrite();

    public synchronized static Brain getBrain(final GraphWrapper wrapper)
            throws Brain.BrainException {

        Brain brain = brains.get(wrapper.getGraph());

        if (null == brain) {
            logger.info("instantiating Extend-o-Brain with base graph " + wrapper.getGraph());
            TopicGraph bg = new PGTopicGraph(wrapper);
            brain = new Brain(bg);
            brain.startBackgroundTasks();
            brains.put(wrapper.getGraph(), brain);
        }

        return brain;
    }

    public synchronized static GraphWrapper getWrapper(final Graph graph) {
        GraphWrapper wrapper = wrappers.get(graph);

        if (null == wrapper) {
            wrapper = wrap(graph);
            wrappers.put(graph, wrapper);
        }

        return wrapper;
    }

    private static GraphWrapper wrap(final Graph graph) {
        return graph instanceof TinkerGraph
            ? new TinkerGraphWrapper((TinkerGraph) graph)
            : new Neo4jGraphWrapper((Neo4jGraph) graph);
    }

    public void handleRequest(final ActionContext context) {

        long before = System.currentTimeMillis();
        wrapTransactionAndExceptions(context);
        long after = System.currentTimeMillis();

        SemanticSynchrony.getLogger().log(Level.INFO, "completed " + getClass().getSimpleName()
                + " action in " + (after - before) + " ms");

        logActivity(context);
    }

    protected void addView(final TreeNode<Link> view,
                           final ActionContext context) throws IOException {
        JSONObject json = context.getJsonPrinter().toJson(view);

        context.getMap().put(Params.VIEW, json);
    }

    public static ActionContext createContext(final Graph graph) {
        ActionContext context = new ActionContext();

        context.setMap(new HashMap<>());

        setGraphWrapper(context, graph);
        setBrain(context);
        setIO(context);

        return context;
    }

    protected void addToHistory(final AtomId rootId) {
        history.visit(rootId);
    }

    protected Iterable<Note> getHistory(final TopicGraph graph,
                                        final Filter filter) {
        return history.getHistory(100, graph, filter);
    }

    private void wrapTransactionAndExceptions(final ActionContext context) {
        setTitle(context, "[no title]");

        try {
            TopicGraph.wrapInTransaction(context.getBrain().getTopicGraph(), () -> performTransaction(context));
        } catch (Exception e) {
            // Gremlin Server does not necessarily print the full stack trace,
            // so we print it here before propagating the exception.
            e.printStackTrace(System.err);

            throw new RequestProcessingException(e);
        }
    }

    private void logActivity(final ActionContext context) {
        // Note: currently, all activities are logged, but the log is not immediately flushed
        //       unless the transaction succeeds.
        if (null != context.getBrain().getActivityLog()) {
            context.getBrain().getActivityLog().flush();
        }
    }

    private static void setBrain(final ActionContext context) {
        try {
            context.setBrain(getBrain(context.getGraphWrapper()));
        } catch (Brain.BrainException e) {
            throw new RequestProcessingException(e);
        }
    }

    private static void setIO(final ActionContext context) {
        context.setQueries(new TreeViews(context.getBrain()));
        context.setWikiParser(new WikiParser());
        context.setJsonParser(new JsonParser());
        context.setJsonPrinter(new JsonPrinter());
    }

    private static void setGraphWrapper(final ActionContext context, final Graph graph) {
        GraphWrapper wrapper = getWrapper(graph);
        context.setGraphWrapper(wrapper);
    }

    protected void setTitle(final ActionContext context, final String title) {
        context.getMap().put(Params.VIEW_TITLE, title);
    }

    protected <T> T notNull(T object) {
        Preconditions.checkNotNull(object, "action is missing a required field");
        return object;
    }
}
