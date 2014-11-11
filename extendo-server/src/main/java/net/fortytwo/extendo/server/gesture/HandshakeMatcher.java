package net.fortytwo.extendo.server.gesture;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Logger;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class HandshakeMatcher {
    private static final Logger logger = Logger.getLogger(HandshakeMatcher.class.getName());

    // should match the constant used by Extend-o-Hand for handshake peak recognition
    private static final long PEAK_MAX_GAP = 353;

    private static final int STACK_SIZE_WARN_THRESHOLD = 1000;
    private long lastWarning = 0;

    private final Map<String, Handshake> handshakesByActor;
    private final Stack<Handshake> latestHandshakes;
    private Collection<Handshake> cleanupBuffer = new LinkedList<Handshake>();

    private final HandshakeHandler handler;

    public HandshakeMatcher(HandshakeHandler handler) {
        this.handler = handler;
        this.handshakesByActor = new HashMap<String, Handshake>();
        this.latestHandshakes = new Stack<Handshake>();
    }

    public void reset() {
        handshakesByActor.clear();
        latestHandshakes.clear();
    }

    private boolean isOld(final Handshake h,
                          final long now) {
        return now - h.latestPeak > PEAK_MAX_GAP;
    }

    private void cleanup(final long timestamp) {
        for (Handshake h : latestHandshakes) {
            if (isOld(h, timestamp)) {
                cleanupBuffer.add(h);
            }
        }

        if (cleanupBuffer.size() > 0) {
            for (Handshake h : cleanupBuffer) {
                latestHandshakes.remove(h);
                handshakesByActor.remove(h.actor);
            }

            cleanupBuffer.clear();
        }
    }

    public synchronized void receiveEvent(final String actor,
                                          final long timestamp) {
        //System.out.println("receiveEvent: " + actor + ", " + timestamp);
        cleanup(timestamp);

        Handshake shake = handshakesByActor.get(actor);
        boolean isNew = false;

        if (null == shake) {
            shake = new Handshake();
            shake.actor = actor;
            shake.firstPeak = timestamp;
            handshakesByActor.put(actor, shake);
            isNew = true;
        } else if (timestamp < shake.latestPeak) {
            throw new IllegalStateException("handshake peaks of actor " + actor + " arrived out of order ("
                    + timestamp + "<=" + shake.latestPeak + ")");
        }

        shake.peaks.add(timestamp);
        shake.latestPeak = timestamp;

        if (!shake.matched) {
            for (Handshake h : latestHandshakes) {
                if (h.matches(shake)) {
                    handler.handle(shake, h);

                    // mark both handshakes as matched and leave them in the stack,
                    // so we don't match them again
                    h.matched = true;
                    shake.matched = true;

                    break;
                }
            }
        }

        if (isNew) {
            latestHandshakes.add(shake);
            if (latestHandshakes.size() > STACK_SIZE_WARN_THRESHOLD) {
                long now = System.currentTimeMillis();
                if (now - lastWarning > 10000) {
                    logger.warning("gestural stack is suspiciously large");
                    lastWarning = now;
                }
            }

            /*System.out.println("current stack:");
            for (Handshake h : latestHandshakes) {
                System.out.println("\t" + h);
            }*/
        }
    }

    public interface HandshakeHandler {
        void handle(Handshake left, Handshake right);
    }

    public class Handshake {
        public String actor;
        public final List<Long> peaks = new LinkedList<Long>();
        public long firstPeak;
        public long latestPeak;
        private boolean matched = false;

        public boolean matches(Handshake other) {
            return !other.actor.equals(actor)
                    && !matched && !other.matched
                    && ((other.firstPeak >= firstPeak && other.firstPeak <= latestPeak
                    && latestPeak >= other.firstPeak && latestPeak <= other.latestPeak)
                    || (firstPeak >= other.firstPeak && firstPeak <= other.latestPeak
                    && other.latestPeak >= firstPeak && other.latestPeak <= latestPeak));
        }
    }
}
