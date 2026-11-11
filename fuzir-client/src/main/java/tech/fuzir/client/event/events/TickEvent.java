package tech.fuzir.client.event.events;

/**
 * Posted at the end of every client tick while a world and player are present.
 */
public final class TickEvent {
    public static final TickEvent INSTANCE = new TickEvent();

    private TickEvent() {
    }
}
