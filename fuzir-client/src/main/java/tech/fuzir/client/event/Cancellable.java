package tech.fuzir.client.event;

/**
 * Base class for events whose further propagation (and optionally the vanilla behaviour) can be stopped.
 */
public abstract class Cancellable {
    private boolean cancelled;

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }
}
