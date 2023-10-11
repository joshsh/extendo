package net.fortytwo.smsn.brain.model;

import net.fortytwo.smsn.brain.AtomId;
import net.fortytwo.smsn.brain.model.entities.Note;
import net.fortytwo.smsn.brain.model.entities.ListNode;
import net.fortytwo.smsn.brain.model.entities.TreeNode;
import net.fortytwo.smsn.brain.model.entities.Link;
import net.fortytwo.smsn.brain.model.entities.Page;
import net.fortytwo.smsn.brain.model.entities.Topic;

import java.util.List;
import java.util.Optional;

/**
 * A graph of notes and lists conforming to the SmSn data model
 */
public interface TopicGraph {

    Iterable<Note> getAllNotes();

    Optional<Note> getNoteById(AtomId id);

    List<Note> getNotesByAcronym(String acronym, Filter filter);

    List<Note> getNotesByShortcut(String shortcut, Filter filter);

    List<Note> getNotesByTitleQuery(String value, Filter filter);

    AtomId idOf(Note a);

    String iriOf(Note a);

    Topic createTopic(AtomId id);

    Page createPage(Link root);

    Link createLink(Topic target, String label, Role role);

    TreeNode<Link> createTopicTree(Link link);

    Note createNote(AtomId id);

    Note createNoteWithProperties(Filter filter, AtomId id);

    ListNode<Link> toList(Link... elements);

    ListNode<Topic> toList(Topic... elements);

    ListNode<TreeNode<Link>> toList(TreeNode<Link>... elements);

    ListNode<Note> createListOfNotes(Note... elements);

    void removeIsolatedNotes(Filter filter);

    void notifyOfUpdate();

    void reindex(Note a);

    long getLastUpdate();

    void begin();

    void commit();

    void rollback();

    TopicGraph createFilteredGraph(Filter filter);

    interface IORunnable {
        void run();
    }

    static void wrapInTransaction(final TopicGraph graph, final IORunnable runnable) {
        graph.begin();

        boolean success = false;
        try {
            runnable.run();
            success = true;
        } finally {
            if (success) {
                graph.commit();
            } else {
                graph.rollback();
            }
        }
    }
}
