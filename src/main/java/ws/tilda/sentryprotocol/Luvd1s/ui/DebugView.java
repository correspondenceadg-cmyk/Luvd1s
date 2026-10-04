package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.service.DebugService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

import java.util.Map;

@PermitAll
@Route("debug")
@PageTitle("Debug")
@StyleSheet("context://styles/debug.css")
public class DebugView extends VerticalLayout {

    private final DebugService debugService;
    private final AuthenticationContext authContext;

    private String crtMode = "subtle";

    public DebugView(DebugService debugService, AuthenticationContext authContext) {
        this.debugService = debugService;
        this.authContext = authContext;

        setPadding(false);
        setSpacing(true);
        getStyle().set("padding", "12px");
        getStyle().set("box-sizing", "border-box");
        setWidthFull();

        add(buildHeader());
        add(buildJvmCard());
        add(buildDatabaseCard());
        add(buildSystemCard());
        add(buildApiCard());

        add(buildCrtOverlay());
        add(buildBootOverlay());
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);

        UI.getCurrent().getPage().executeJs(
                "const stored = localStorage.getItem('luvd1s-crt-mode');" +
                "const mode = stored || 'subtle';" +
                "document.documentElement.setAttribute('data-crt-mode', mode);" +
                "return mode;"
        ).then(String.class, mode -> this.crtMode = mode);

        UI.getCurrent().getPage().executeJs(BOOT_JS);
    }

    private void applyCrtMode(String mode) {
        this.crtMode = mode;
        UI.getCurrent().getPage().executeJs(
                "document.documentElement.setAttribute('data-crt-mode', $0);" +
                "localStorage.setItem('luvd1s-crt-mode', $0);",
                mode
        );
    }

    private HorizontalLayout buildHeader() {
        H2 title = new H2("Debug");
        title.getStyle().set("margin", "0");

        Button people = new Button("People", VaadinIcon.USERS.create(),
                e -> UI.getCurrent().navigate(PeopleView.class));
        people.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button dashboard = new Button("Dashboard", VaadinIcon.CHART.create(),
                e -> UI.getCurrent().navigate(DashboardView.class));
        dashboard.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button crtToggle = new Button("CRT: Subtle", VaadinIcon.EYE.create());
        crtToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        crtToggle.setAriaLabel("Cycle CRT display mode");
        crtToggle.addClickListener(e -> {
            String next = switch (crtMode) {
                case "off" -> "subtle";
                case "subtle" -> "full";
                default -> "off";
            };
            applyCrtMode(next);
            crtToggle.setText("CRT: " + capitalize(next));
        });

        Button logout = new Button("Log out", e -> authContext.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttons = new HorizontalLayout(people, dashboard, crtToggle, logout);
        buttons.setSpacing(true);
        buttons.setAlignItems(Alignment.CENTER);
        buttons.getStyle().set("flex-wrap", "wrap");
        buttons.getStyle().set("gap", "4px");

        HorizontalLayout header = new HorizontalLayout(title, buttons);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle().set("flex-wrap", "wrap");
        header.getStyle().set("gap", "8px");
        return header;
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private Div buildJvmCard() {
        Div card = new Div();
        card.addClassName("debug-card");
        card.setWidthFull();

        H3 heading = new H3("JVM");
        heading.getStyle().set("margin", "0 0 12px 0");
        card.add(heading);

        for (Map.Entry<String, String> e : debugService.jvmStats().entrySet()) {
            card.add(statRow(e.getKey(), e.getValue()));
        }
        return card;
    }

    private Div buildDatabaseCard() {
        Div card = new Div();
        card.addClassName("debug-card");
        card.setWidthFull();

        H3 heading = new H3("Database");
        heading.getStyle().set("margin", "0 0 12px 0");
        card.add(heading);

        for (Map.Entry<String, Long> e : debugService.rowCounts().entrySet()) {
            card.add(statRow(e.getKey(), e.getValue() + " rows"));
        }
        return card;
    }

    private Div buildSystemCard() {
        Div card = new Div();
        card.addClassName("debug-card");
        card.setWidthFull();

        H3 heading = new H3("System");
        heading.getStyle().set("margin", "0 0 12px 0");
        card.add(heading);

        for (Map.Entry<String, String> e : debugService.systemStats().entrySet()) {
            card.add(statRow(e.getKey(), e.getValue()));
        }
        return card;
    }

    private Div buildApiCard() {
        Div card = new Div();
        card.addClassName("debug-card");
        card.setWidthFull();

        H3 heading = new H3("API");
        heading.getStyle().set("margin", "0 0 12px 0");
        card.add(heading);

        Span description = new Span("Interactive documentation for the REST endpoints.");
        description.getStyle()
                .set("display", "block")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "12px");
        card.add(description);

        Button swagger = new Button("Open Swagger UI", VaadinIcon.EXTERNAL_LINK.create(),
                e -> UI.getCurrent().getPage().open("/swagger-ui.html", "_blank"));
        swagger.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        swagger.setAriaLabel("Open Swagger UI in a new tab");
        card.add(swagger);
        return card;
    }

    private HorizontalLayout statRow(String label, String value) {
        Span labelSpan = new Span(label);
        labelSpan.addClassName("stat-label");
        labelSpan.getStyle()
                .set("font-weight", "500")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("min-width", "140px")
                .set("text-transform", "capitalize");

        Span valueSpan = new Span(value);
        valueSpan.addClassName("stat-value");
        valueSpan.getStyle()
                .set("font-family", "ui-monospace, SFMono-Regular, monospace")
                .set("color", "var(--lumo-body-text-color)");

        HorizontalLayout row = new HorizontalLayout(labelSpan, valueSpan);
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);
        row.getStyle().set("padding", "4px 0");
        return row;
    }

    private Div buildCrtOverlay() {
        Div overlay = new Div();
        overlay.addClassName("crt-overlay");
        overlay.getElement().setAttribute("aria-hidden", "true");

        Div flicker = new Div();
        flicker.addClassName("crt-flicker");
        flicker.getElement().setAttribute("aria-hidden", "true");
        overlay.add(flicker);

        return overlay;
    }

    private Div buildBootOverlay() {
        Div overlay = new Div();
        overlay.getElement().setAttribute("id", "debug-boot-overlay");
        overlay.getElement().setAttribute("aria-hidden", "true");

        Div lines = new Div();
        lines.addClassName("boot-lines");
        overlay.add(lines);

        return overlay;
    }

    private static final String BOOT_JS =
        "(function() {" +
        "  const overlay = document.getElementById('debug-boot-overlay');" +
        "  if (!overlay) return;" +
        "  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;" +
        "  if (reduced) { overlay.style.display = 'none'; return; }" +
        "  if (sessionStorage.getItem('luvd1s-boot-played') === 'true') {" +
        "    overlay.style.display = 'none'; return;" +
        "  }" +
        "  const container = overlay.querySelector('.boot-lines');" +
        "  if (!container) return;" +
        "  const delay = ms => new Promise(r => setTimeout(r, ms));" +
        "  async function typeText(el, text, msPerChar) {" +
        "    for (let i = 0; i < text.length; i++) {" +
        "      el.textContent += text[i];" +
        "      await delay(msPerChar);" +
        "    }" +
        "  }" +
        "  async function cycleEllipsis(el, loops) {" +
        "    for (let i = 0; i < loops; i++) {" +
        "      for (let d = 0; d <= 3; d++) {" +
        "        el.textContent = '.'.repeat(d);" +
        "        await delay(220);" +
        "      }" +
        "    }" +
        "    el.textContent = '...';" +
        "  }" +
        "  async function run() {" +
        "    const lines = [" +
        "      { text: 'System boot',         loops: 3, typewriter: true  }," +
        "      { text: 'System Inspecting',   loops: 3, typewriter: false }," +
        "      { text: 'System Initializing', loops: 2, typewriter: false }" +
        "    ];" +
        "    for (const line of lines) {" +
        "      const lineEl = document.createElement('div');" +
        "      lineEl.className = 'boot-line';" +
        "      const textEl = document.createElement('span');" +
        "      textEl.className = 'boot-text';" +
        "      const dotsEl = document.createElement('span');" +
        "      dotsEl.className = 'boot-dots';" +
        "      lineEl.appendChild(textEl);" +
        "      lineEl.appendChild(dotsEl);" +
        "      container.appendChild(lineEl);" +
        "      if (line.typewriter) {" +
        "        await typeText(textEl, line.text, 55);" +
        "      } else {" +
        "        textEl.textContent = line.text;" +
        "      }" +
        "      await delay(150);" +
        "      await cycleEllipsis(dotsEl, line.loops);" +
        "      await delay(250);" +
        "    }" +
        "    await delay(500);" +
        "    overlay.classList.add('boot-fadeout');" +
        "    await delay(800);" +
        "    overlay.style.display = 'none';" +
        "    sessionStorage.setItem('luvd1s-boot-played', 'true');" +
        "  }" +
        "  run();" +
        "})();";
}