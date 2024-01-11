package net.fortytwo.smsn.brain;

import net.fortytwo.smsn.SemanticSynchrony;
import net.fortytwo.smsn.brain.model.TopicGraph;
import net.fortytwo.smsn.brain.rdf.KnowledgeBase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Brain {

    /**
     * A configuration property indicating a special note to which notes may be prepended
     * in a stream-of-consciousness style
     */
    public static final String PROP_BRAINSTREAM = "net.fortytwo.smsn.brain.brainStream";

    // TODO: make this configurable
    private static final int EVENT_STACK_CAPACITY = 50;

    // TODO: make these configurable
    private static final long
            INFERENCE_PERIOD = 1000L * 60,
            INFERENCE_INITIAL_WAIT = 1000L * 30;

    private static final boolean RUN_BACKGROUND_TASKS = false;

    private final TopicGraph topicGraph;

    private final KnowledgeBase knowledgeBase;

    private final ActivityLog activityLog;

    private final Priorities priorities;

    private final EventStack eventStack;

    public Brain(final TopicGraph topicGraph) throws BrainException {
        this.topicGraph = topicGraph;

        knowledgeBase = new KnowledgeBase(topicGraph);

        try {
            knowledgeBase.addDefaultClasses();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BrainException(e);
        }

        String filePath = SemanticSynchrony.getConfiguration().getActivityLog();

        if (null == filePath) {
            SemanticSynchrony.getLogger().warning("no activity log specified");
            activityLog = null;
        } else {
            SemanticSynchrony.getLogger().fine("using activity log at " + filePath);
            try {
                File logFile = new File(filePath);
                createDirectories(logFile);
                activityLog = new ActivityLog(new FileWriter(logFile, true));
            } catch (IOException e) {
                throw new BrainException(e);
            }
        }

        priorities = new Priorities();

        eventStack = new EventStack(EVENT_STACK_CAPACITY);
    }

    public void startBackgroundTasks() {
        if (!RUN_BACKGROUND_TASKS) return;

        priorities.refreshQueue(topicGraph);

        knowledgeBase.inferAutomatically(INFERENCE_INITIAL_WAIT, INFERENCE_PERIOD);
    }

    public TopicGraph getTopicGraph() {
        return topicGraph;
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public ActivityLog getActivityLog() {
        return activityLog;
    }

    public Priorities getPriorities() {
        return priorities;
    }

    public EventStack getEventStack() {
        return eventStack;
    }

    public class BrainException extends Exception {
        public BrainException(final Throwable cause) {
            super(cause);
        }
    }

    private void createDirectories(final File file) {
        file.getParentFile().mkdirs();
    }
}
