package ws.tilda.sentryprotocol.Luvd1s.ui.components;

import com.vaadin.flow.component.Component;

/**
 * One-shot visual feedback animations. Each method adds a CSS class, forces
 * a reflow so the animation replays even if called twice in a row, then
 * removes the class when the animation ends.
 */
public final class Feedback {

    private Feedback() {
    }

    public static void pulseSuccess(Component component) {
        play(component, "micro-pulse-success");
    }

    public static void shake(Component component) {
        play(component, "micro-shake");
    }

    public static void fadeRefresh(Component component) {
        play(component, "micro-fade");
    }

    private static void play(Component component, String className) {
        if (component == null) return;
        component.getElement().executeJs(
                "const el = this;" +
                "const cls = $0;" +
                "el.classList.remove(cls);" +
                "void el.offsetWidth;" +
                "el.classList.add(cls);" +
                "el.addEventListener('animationend', () => el.classList.remove(cls), { once: true });",
                className
        );
    }
}