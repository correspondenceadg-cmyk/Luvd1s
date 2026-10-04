package ws.tilda.sentryprotocol.Luvd1s.ui.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;

@SuppressWarnings("removal")
public class ThemeToggle extends Button {

    private static final String TOGGLE_JS =
            "const html = document.documentElement; " +
            "const isDark = html.getAttribute('theme') === 'dark'; " +
            "if (isDark) { " +
            "  html.removeAttribute('theme'); " +
            "  localStorage.setItem('luvd1s-theme', 'light'); " +
            "  return false; " +
            "} else { " +
            "  html.setAttribute('theme', 'dark'); " +
            "  localStorage.setItem('luvd1s-theme', 'dark'); " +
            "  return true; " +
            "}";

    private static final String READ_JS =
            "return document.documentElement.getAttribute('theme') === 'dark';";

    public ThemeToggle() {
        addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        setIcon(VaadinIcon.MOON_O.create());
        setAriaLabel("Switch to dark mode");
        setTooltipText("Switch to dark mode");
        addClickListener(e -> toggle());
    }

    private void toggle() {
        UI.getCurrent().getPage().executeJs(TOGGLE_JS)
                .then(Boolean.class, this::applyIcon);
    }

    private void applyIcon(boolean isDark) {
        setIcon(isDark ? VaadinIcon.SUN_O.create() : VaadinIcon.MOON_O.create());
        String label = isDark ? "Switch to light mode" : "Switch to dark mode";
        setAriaLabel(label);
        setTooltipText(label);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI.getCurrent().getPage().executeJs(READ_JS)
                .then(Boolean.class, this::applyIcon);
    }
}