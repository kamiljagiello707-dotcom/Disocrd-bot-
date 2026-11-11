package tech.fuzir.client.event.events;

import tech.fuzir.client.event.Cancellable;

/**
 * Raw GLFW keyboard input, posted before vanilla handling. Cancelling suppresses the vanilla key handler.
 */
public final class KeyEvent extends Cancellable {
    private final int key;
    private final int scancode;
    private final int action;
    private final int modifiers;

    public KeyEvent(int key, int scancode, int action, int modifiers) {
        this.key = key;
        this.scancode = scancode;
        this.action = action;
        this.modifiers = modifiers;
    }

    public int key() {
        return key;
    }

    public int scancode() {
        return scancode;
    }

    public int action() {
        return action;
    }

    public int modifiers() {
        return modifiers;
    }
}
