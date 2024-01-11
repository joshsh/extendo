package net.fortytwo.smsn.server.actions;

import net.fortytwo.smsn.server.ActionContext;
import net.fortytwo.smsn.server.errors.BadRequestException;
import net.fortytwo.smsn.server.errors.RequestProcessingException;

/**
 * A service for removing isolated notes (i.e. notes with neither parents nor children) from an Extend-o-Brain graph
 */
public class RemoveIsolatedNotes extends FilteredAction {

    @Override
    protected void performTransaction(final ActionContext params) throws RequestProcessingException, BadRequestException {
        params.getBrain().getTopicGraph().removeIsolatedNotes(getFilter());
    }

    @Override
    protected boolean doesRead() {
        return true;
    }

    @Override
    protected boolean doesWrite() {
        return true;
    }
}
