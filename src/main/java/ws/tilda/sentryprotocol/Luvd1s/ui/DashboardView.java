package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.Tag;
import ws.tilda.sentryprotocol.Luvd1s.service.InteractionService;
import ws.tilda.sentryprotocol.Luvd1s.service.PersonService;
import ws.tilda.sentryprotocol.Luvd1s.service.TagService;
import ws.tilda.sentryprotocol.Luvd1s.ui.components.SkeletonViews;
import ws.tilda.sentryprotocol.Luvd1s.ui.components.ThemeToggle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@PermitAll
@Route("dashboard")
@PageTitle("Dashboard")
public class DashboardView extends VerticalLayout {

    private static final int STALE_DAYS = 30;

    private final PersonService personService;
    private final InteractionService interactionService;
    private final TagService tagService;
    private final AuthenticationContext authContext;

    private final VerticalLayout content = new VerticalLayout();

    public DashboardView(PersonService personService,
                         InteractionService interactionService,
                         TagService tagService,
                         AuthenticationContext authContext) {
        this.personService = personService;
        this.interactionService = interactionService;
        this.tagService = tagService;
        this.authContext = authContext;

        setPadding(true);
        setSpacing(true);

        add(buildHeader());

        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();
        content.add(buildSkeleton());
        add(content);

        loadAsync();
    }

    private void loadAsync() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UI ui = UI.getCurrent();

        CompletableFuture.runAsync(() -> {
            SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                List<Person> people = personService.findAll();

                Map<Long, Integer> interactionCounts = new LinkedHashMap<>();
                for (Person p : people) {
                    interactionCounts.put(p.getId(), interactionService.findByPerson(p).size());
                }

                List<Tag> tags = tagService.findAll();

                ui.access(() -> {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    try {
                        renderDashboard(people, interactionCounts, tags);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    private VerticalLayout buildSkeleton() {
        VerticalLayout skeleton = new VerticalLayout();
        skeleton.setPadding(false);
        skeleton.setSpacing(true);
        skeleton.setWidthFull();

        skeleton.add(SkeletonViews.statCards(4));
        skeleton.add(new H3("Contacts by company"));
        skeleton.add(SkeletonViews.barChart(3));
        skeleton.add(new H3("Contacts by tag"));
        skeleton.add(SkeletonViews.barChart(4));
        skeleton.add(new H3("Birthdays this month"));
        skeleton.add(SkeletonViews.cardList(2));
        skeleton.add(new H3("Reach out soon"));
        skeleton.add(SkeletonViews.cardList(3));

        return skeleton;
    }

    private void renderDashboard(List<Person> people,
                                 Map<Long, Integer> interactionCounts,
                                 List<Tag> tags) {
        LocalDate today = LocalDate.now();

        content.removeAll();
        content.add(buildStatCards(people, today, interactionCounts));

        if (people.isEmpty()) {
            content.add(emptySection(
                    "No contacts yet",
                    "Add your first contact on the People page to see breakdowns here."
            ));
        } else {
            content.add(new H3("Contacts by company"));
            content.add(buildBarChart(groupByCompany(people)));

            content.add(new H3("Contacts by tag"));
            content.add(buildBarChart(groupByTag(people, tags)));
        }

        content.add(new H3("Birthdays this month"));
        content.add(buildBirthdayList(people, today.getMonth()));

        content.add(new H3("Reach out soon"));
        content.add(buildStaleList(people, today));
    }

    private HorizontalLayout buildHeader() {
        H2 title = new H2("Dashboard");

        Button peopleButton = new Button("People", VaadinIcon.USERS.create(),
                e -> UI.getCurrent().navigate(PeopleView.class));
        peopleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        peopleButton.setAriaLabel("Go to people list");

        ThemeToggle themeToggle = new ThemeToggle();

        Button logoutButton = new Button("Log out", e -> authContext.logout());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logoutButton.setAriaLabel("Log out of your account");

        HorizontalLayout buttons = new HorizontalLayout(peopleButton, themeToggle, logoutButton);
        buttons.setSpacing(true);
        buttons.setAlignItems(Alignment.CENTER);

        HorizontalLayout header = new HorizontalLayout(title, buttons);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        return header;
    }

    private HorizontalLayout buildStatCards(List<Person> people,
                                            LocalDate today,
                                            Map<Long, Integer> interactionCounts) {
        long total = people.size();
        long birthdays = people.stream()
                .filter(p -> p.getBirthday() != null && p.getBirthday().getMonth() == today.getMonth())
                .count();
        long stale = people.stream().filter(p -> isStale(p, today)).count();
        long interactions = interactionCounts.values().stream().mapToLong(Integer::longValue).sum();

        HorizontalLayout row = new HorizontalLayout(
                statCard("Contacts", total, "#2196F3"),
                statCard("Interactions", interactions, "#4CAF50"),
                statCard("Birthdays", birthdays, "#E91E63"),
                statCard("Stale", stale, "#FF9800")
        );
        row.setWidthFull();
        row.getStyle().set("flex-wrap", "wrap");
        row.getStyle().set("gap", "12px");
        return row;
    }

    private Div statCard(String label, long value, String color) {
        Div card = new Div();
        card.getStyle()
                .set("flex", "1 1 130px")
                .set("min-width", "130px")
                .set("padding", "16px")
                .set("border-radius", "8px")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-left", "4px solid " + color);

        Span valueSpan = new Span(String.valueOf(value));
        valueSpan.getStyle()
                .set("display", "block")
                .set("font-size", "1.8em")
                .set("font-weight", "700")
                .set("color", color);

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("display", "block")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.85em")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px");

        card.add(valueSpan, labelSpan);
        card.getElement().setAttribute("aria-label", value + " " + label);
        return card;
    }

    private Map<String, Long> groupByCompany(List<Person> people) {
        return people.stream()
                .filter(p -> p.getCompany() != null && !p.getCompany().isBlank())
                .collect(Collectors.groupingBy(
                        Person::getCompany,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private Map<String, Long> groupByTag(List<Person> people, List<Tag> tags) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Tag tag : tags) {
            long count = people.stream()
                    .filter(p -> p.getTags().contains(tag))
                    .count();
            if (count > 0) {
                counts.put(tag.getName(), count);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private VerticalLayout buildBarChart(Map<String, Long> data) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(true);
        container.setWidthFull();

        if (data.isEmpty()) {
            container.add(emptyInline("No data to show yet."));
            return container;
        }

        long max = data.values().stream().max(Long::compare).orElse(1L);
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            container.add(barRow(entry.getKey(), entry.getValue(), max, "#2196F3"));
        }
        return container;
    }

    private HorizontalLayout barRow(String label, long value, long max, String color) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("width", "100px")
                .set("font-size", "0.9em")
                .set("flex-shrink", "0");

        Div track = new Div();
        track.getStyle()
                .set("background-color", "var(--lumo-contrast-10pct)")
                .set("height", "20px")
                .set("border-radius", "10px")
                .set("flex", "1")
                .set("overflow", "hidden");
        track.getElement().setAttribute("aria-label", label + ": " + value);

        Div fill = new Div();
        double percent = max == 0 ? 0 : (value * 100.0) / max;
        fill.getStyle()
                .set("background-color", color)
                .set("height", "100%")
                .set("width", percent + "%")
                .set("border-radius", "10px");
        track.add(fill);

        Span valueSpan = new Span(String.valueOf(value));
        valueSpan.getStyle()
                .set("width", "30px")
                .set("text-align", "right")
                .set("font-weight", "600")
                .set("color", "var(--lumo-body-text-color)")
                .set("flex-shrink", "0");

        HorizontalLayout row = new HorizontalLayout(labelSpan, track, valueSpan);
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.setSpacing(true);
        return row;
    }

    private VerticalLayout buildBirthdayList(List<Person> people, Month month) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        container.setWidthFull();

        List<Person> birthdayPeople = people.stream()
                .filter(p -> p.getBirthday() != null && p.getBirthday().getMonth() == month)
                .sorted(Comparator.comparing(p -> p.getBirthday().getDayOfMonth()))
                .toList();

        if (birthdayPeople.isEmpty()) {
            container.add(emptyInline("No birthdays in "
                    + month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + "."));
            return container;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        for (Person p : birthdayPeople) {
            Span name = new Span(p.getFirstName() + " " + p.getLastName());
            name.getStyle().set("font-weight", "500");

            Span date = new Span(p.getBirthday().format(fmt));
            date.getStyle().set("color", "#E91E63").set("font-weight", "600");

            HorizontalLayout row = new HorizontalLayout(name, date);
            row.setWidthFull();
            row.setJustifyContentMode(JustifyContentMode.BETWEEN);
            row.setPadding(false);
            row.getStyle()
                    .set("padding", "8px 12px")
                    .set("border-left", "3px solid #E91E63")
                    .set("background-color", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "4px")
                    .set("margin-bottom", "6px");

            container.add(row);
        }
        return container;
    }

    private VerticalLayout buildStaleList(List<Person> people, LocalDate today) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        container.setWidthFull();

        List<Person> stale = people.stream()
                .filter(p -> isStale(p, today))
                .sorted(Comparator.comparing(p -> p.getLastContactedAt() == null
                        ? LocalDateTime.MIN
                        : p.getLastContactedAt()))
                .toList();

        if (stale.isEmpty()) {
            if (people.isEmpty()) {
                container.add(emptyInline("Add contacts to start tracking who to reach out to."));
            } else {
                container.add(emptyInline("Everyone's been contacted recently. Nice work."));
            }
            return container;
        }

        for (Person p : stale) {
            Span name = new Span(p.getFirstName() + " " + p.getLastName());
            name.getStyle().set("font-weight", "500");

            long days = p.getLastContactedAt() == null
                    ? -1
                    : java.time.temporal.ChronoUnit.DAYS.between(
                            p.getLastContactedAt().toLocalDate(), today);

            Span status = new Span(days < 0 ? "Never contacted" : days + " days ago");
            status.getStyle()
                    .set("color", "#FF9800")
                    .set("font-weight", "600")
                    .set("font-size", "0.85em");

            Button open = new Button("Open", e ->
                    UI.getCurrent().navigate(PersonDetailView.class, p.getId()));
            open.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            open.setAriaLabel("Open " + p.getFirstName() + " " + p.getLastName());

            HorizontalLayout row = new HorizontalLayout(name, status, open);
            row.setWidthFull();
            row.setAlignItems(Alignment.CENTER);
            row.setJustifyContentMode(JustifyContentMode.BETWEEN);
            row.getStyle()
                    .set("padding", "8px 12px")
                    .set("border-left", "3px solid #FF9800")
                    .set("background-color", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "4px")
                    .set("margin-bottom", "6px");

            container.add(row);
        }
        return container;
    }

    private Span emptyInline(String message) {
        Span span = new Span(message);
        span.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-style", "italic")
                .set("display", "block")
                .set("padding", "8px 0");
        return span;
    }

    private VerticalLayout emptySection(String title, String subtitle) {
        VerticalLayout box = new VerticalLayout();
        box.setPadding(true);
        box.setSpacing(false);
        box.setWidthFull();
        box.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border", "1px dashed var(--lumo-contrast-20pct)")
                .set("border-radius", "8px")
                .set("text-align", "center");

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-weight", "600")
                .set("color", "var(--lumo-body-text-color)")
                .set("display", "block");

        Span subtitleSpan = new Span(subtitle);
        subtitleSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.9em")
                .set("display", "block");

        box.add(titleSpan, subtitleSpan);
        box.setAlignItems(Alignment.CENTER);
        return box;
    }

    private boolean isStale(Person person, LocalDate today) {
        if (person.getLastContactedAt() == null) return true;
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                person.getLastContactedAt().toLocalDate(), today);
        return days >= STALE_DAYS;
    }
}