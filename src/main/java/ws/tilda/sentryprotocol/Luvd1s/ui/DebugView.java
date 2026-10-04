package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.data.AuditLog;
import ws.tilda.sentryprotocol.Luvd1s.service.DebugService;
import ws.tilda.sentryprotocol.Luvd1s.service.RequestRecord;
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
import jakarta.annotation.security.RolesAllowed;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RolesAllowed("ADMIN")
@Route("debug")
@PageTitle("Debug")
@StyleSheet("context://styles/debug.css")
public class DebugView extends VerticalLayout {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

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
        add(buildRecentRequestsCard());
        add(buildAuditCard());
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

    private Div buildRecentRequestsCard() {
        Div card = new Div();
        card.addClassName("debug-card");
        card.setWidthFull();

        H3 heading = new H3("Recent requests");
        heading.getStyle().set("margin", "0 0 12px 0");
        card.add(heading);

        List<RequestRecord> records = debugService.recentRequests();
        if (records.isEmpty()) {
            card.add(emptyHint("No requests captured yet."));
            return card;
        }

        for (RequestRecord r : records) {
            card.add(requestRow(r));
        }
        return card;
    }

    private HorizontalLayout requestRow(RequestRecord r) {
        Span time = mono(timeAgo(r.timestamp()), "60px");
        Span method = badge(r.method(), methodColor(r.method()), "70px");
        Span path = mono(truncate(r.path(), 34), null);
        path.getStyle().set("flex", "1");
        path.getStyle().set("min-width", "0");
        Span user = mono(r.username() != null ? r.username() : "—", "80px");
        Span status = badge(String.valueOf(r.status()), statusColor(r.status()), "50px");
        Span duration = mono(r.durationMs() + "ms", "65px");
        duration.getStyle().set("text-align", "right");

        HorizontalLayout row = new HorizontalLayout(time, method, path, user, status, duration);
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);
        row.getStyle().set("padding", "6px 0");
        row.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        row.getStyle().set("flex-wrap", "wrap");
        row.getStyle().set("gap", "8px");
        return row;
    }

    private Div buildAuditCard() {
        Div card = new Div();
        card.addClassName("debug-card");
        card.setWidthFull();

        H3 heading = new H3("Audit trail");
        heading.getStyle().set("margin", "0 0 12px 0");
        card.add(heading);

        List<AuditLog> entries = debugService.recentAudits();
        if (entries.isEmpty()) {
            card.add(emptyHint("No auditable actions recorded yet."));
            return card;
        }

        for (AuditLog a : entries) {
            card.add(auditRow(a));
        }
        return card;
    }

    private HorizontalLayout auditRow(AuditLog a) {
        Span time = mono(timeAgo(a.getCreatedAt()), "60px");
        Span user = mono(a.getUsername() != null ? a.getUsername() : "—", "80px");
        Span action = badge(a.getAction(), actionColor(a.getAction()), null);
        Span target = mono(
                a.getTargetType() != null
                        ? a.getTargetType() + (a.getTargetId() != null ? " #" + a.getTargetId() : "")
                        : "—",
                "130px"
        );
        Span details = new Span(a.getDetails() != null ? a.getDetails() : "");
        details.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.85em")
                .set("flex", "1")
                .set("min-width", "0");

        HorizontalLayout row = new HorizontalLayout(time, user, action, target, details);
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);
        row.getStyle().set("padding", "6px 0");
        row.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        row.getStyle().set("flex-wrap", "wrap");
        row.getStyle().set("gap", "8px");
        return row;
    }

    private Span mono(String text, String width) {
        Span span = new Span(text);
        span.getStyle()
                .set("font-family", "ui-monospace, SFMono-Regular, monospace")
                .set("font-size", "0.85em")
                .set("color", "var(--lumo-body-text-color)")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");
        if (width != null) {
            span.getStyle().set("width", width);
            span.getStyle().set("flex-shrink", "0");
        }
        return span;
    }

    private Span badge(String text, String color, String width) {
        Span span = new Span(text);
        span.getStyle()
                .set("background-color", color)
                .set("color", "white")
                .set("padding", "1px 8px")
                .set("border-radius", "10px")
                .set("font-size", "0.7em")
                .set("font-weight", "600")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.3px")
                .set("text-align", "center");
        if (width != null) {
            span.getStyle().set("width", width);
            span.getStyle().set("flex-shrink", "0");
            span.getStyle().set("box-sizing", "border-box");
        }
        return span;
    }

    private String methodColor(String method) {
        return switch (method) {
            case "GET" -> "#2196F3";
            case "POST" -> "#4CAF50";
            case "PUT" -> "#FF9800";
            case "DELETE" -> "#F44336";
            default -> "#666";
        };
    }

    private String statusColor(int status) {
        if (status >= 500) return "#F44336";
        if (status >= 400) return "#FF9800";
        if (status >= 300) return "#9C27B0";
        return "#4CAF50";
    }

    private String actionColor(String action) {
        if (action == null) return "#666";
        if (action.endsWith("_CREATE")) return "#4CAF50";
        if (action.endsWith("_UPDATE")) return "#FF9800";
        if (action.endsWith("_DELETE")) return "#F44336";
        return "#2196F3";
    }

    private String timeAgo(Instant instant) {
        if (instant == null) return "—";
        long seconds = Duration.between(instant, Instant.now()).getSeconds();
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m";
        if (seconds < 86400) return (seconds / 3600) + "h";
        return TIME_FMT.format(instant);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private Div emptyHint(String text) {
        Div hint = new Div();
        hint.setText(text);
        hint.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-style", "italic")
                .set("padding", "8px 0");
        return hint;
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