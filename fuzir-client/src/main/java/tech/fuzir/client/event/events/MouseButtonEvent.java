package tech.fuzir.client.event.events;

/**
 * Raw GLFW mouse button input, posted before vanilla handling.
 */
public record MouseButtonEvent(int button, int action, int modifiers) {
}
