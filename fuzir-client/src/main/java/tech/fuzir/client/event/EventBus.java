package tech.fuzir.client.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Minimal typed, thread-safe event bus. Listeners are dispatched by the exact event class.
 * Copy-on-write lists allow (un)registering from inside a handler (e.g. a module disabling itself).
 */
public final class EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger("Fuzir/EventBus");

    private final Map<Class<?>, List<Listener<?>>> listeners = new ConcurrentHashMap<>();

    public <T> Listener<T> subscribe(Class<T> type, Consumer<T> handler) {
        Listener<T> listener = new Listener<>(type, handler);
        register(listener);
        return listener;
    }

    public void register(Listener<?> listener) {
        List<Listener<?>> list = listeners.computeIfAbsent(listener.type(), k -> new CopyOnWriteArrayList<>());
        if (!list.contains(listener)) list.add(listener);
    }

    public void unregister(Listener<?> listener) {
        List<Listener<?>> list = listeners.get(listener.type());
        if (list != null) list.remove(listener);
    }

    @SuppressWarnings("unchecked")
    public <T> T post(T event) {
        List<Listener<?>> list = listeners.get(event.getClass());
        if (list == null || list.isEmpty()) return event;
        for (Listener<?> listener : list) {
            try {
                ((Listener<T>) listener).handler().accept(event);
            } catch (Throwable t) {
                LOGGER.error("Unhandled exception while dispatching {}", event.getClass().getSimpleName(), t);
            }
            if (event instanceof Cancellable cancellable && cancellable.isCancelled()) break;
        }
        return event;
    }

    /** Identity based so that two lambdas with identical bodies stay distinct registrations. */
    public static final class Listener<T> {
        private final Class<T> type;
        private final Consumer<T> handler;

        public Listener(Class<T> type, Consumer<T> handler) {
            this.type = type;
            this.handler = handler;
        }

        public Class<T> type() {
            return type;
        }

        public Consumer<T> handler() {
            return handler;
        }
    }
}
