package ws.tilda.sentryprotocol.Luvd1s.ui;

import jakarta.annotation.security.PermitAll;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.Tag;
import ws.tilda.sentryprotocol.Luvd1s.service.InteractionService;
import ws.tilda.sentryprotocol.Luvd1s.service.PersonService;
import ws.tilda.sentryprotocol.Luvd1s.service.TagService;
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
import java.util.stream.Collectors;

@Route("dashboard")
@PageTitle("Dashboard")
public class DashboardView extends VerticalLayout {

    private static final int STALE_DAYS = 30;

    private final PersonService personService;
    private final InteractionService interactionService;
    private final TagService tagService;

    public DashboardView(PersonService personService,
                         InteractionService interactionService,
                         TagService tagService) {
        this.personService = personService;
        this.interactionService = interactionService;
        this.tagService = tagService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        List<Person> people = personService.findAll();
        LocalDate today = LocalDate.now();

        add(buildHeader());
        add(buildStatCards(people, today));

        if (!people.isEmpty()) {
            add(new H3("Contacts by company"));
            add(buildBarChart(groupByCompany(people)));

            add(new H3("Contacts by tag"));
            add(buildBarChart(groupByTag(people)));
        }

        add(new H3("Birthdays this month"));
        add(buildBirthdayList(people, today.getMonth()));

        add(new H3("Reach out soon"));
        add(buildStaleList(people, today));
    }

    private HorizontalLayout buildHeader() {
        H2 title = new H2("Dashboard");

        Button peopleButton = new Button("People", VaadinIcon.USERS.create(),
                e -> UI.getCurrent().navigate(PeopleView.class));
        peopleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(title, peopleButton);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        return header;
    }

    private HorizontalLayout buildStatCards(List<Person> people, LocalDate today) {
        long total = people.size();
        long birthdays = people.stream()
                .filter(p -> p.getBirthday() != null
                        && p.getBirthday().getMonth() == today.getMonth())
                .count();
        long stale = people.stream().filter(p -> isStale(p, today)).count();
        long logged = interactionService.findByPerson(null).isEmpty() ? 0 : 0;
        long interactions = countInteractions(people);

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

    private long countInteractions(List<Person> people) {
        long total = 0;
        for (Person p : people) {
            total += interactionService.findByPerson(p).size();
        }
        return total;
    }

    private Div statCard(String label, long value, String color) {
        Div card = new Div();
        card.getStyle()
                .set("flex", "1 1 130px")
                .set("min-width", "130px")
                .set("padding", "16px")
                .set("border-radius", "8px")
                .set("background-color", "#f5f5f5")
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
                .set("color", "#666")
                .set("font-size", "0.85em")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px");

        card.add(valueSpan, labelSpan);
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

    private Map<String, Long> groupByTag(List<Person> people) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Tag tag : tagService.findAll()) {
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
            Span empty = new Span("No data yet.");
            empty.getStyle().set("color", "#888").set("font-style", "italic");
            container.add(empty);
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
                .set("background-color", "#e0e0e0")
                .set("height", "20px")
                .set("border-radius", "10px")
                .set("flex", "1")
                .set("overflow", "hidden");

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
                .set("color", "#333")
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
            Span empty = new Span("No birthdays in " + month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ".");
            empty.getStyle().set("color", "#888").set("font-style", "italic");
            container.add(empty);
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
                    .set("background-color", "#fafafa")
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
            Span empty = new Span("Everyone's been contacted recently.");
            empty.getStyle().set("color", "#888").set("font-style", "italic");
            container.add(empty);
            return container;
        }

        for (Person p : stale) {
            Span name = new Span(p.getFirstName() + " " + p.getLastName());
            name.getStyle().set("font-weight", "500");

            long days = p.getLastContactedAt() == null
                    ? -1
                    : java.time.temporal.ChronoUnit.DAYS.between(
                            p.getLastContactedAt().toLocalDate(), today);

            Span status = new Span(days < 0
                    ? "Never contacted"
                    : days + " days ago");
            status.getStyle()
                    .set("color", "#FF9800")
                    .set("font-weight", "600")
                    .set("font-size", "0.85em");

            Button open = new Button("Open", e ->
                    UI.getCurrent().navigate(PersonDetailView.class, p.getId()));
            open.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            HorizontalLayout row = new HorizontalLayout(name, status, open);
            row.setWidthFull();
            row.setAlignItems(Alignment.CENTER);
            row.setJustifyContentMode(JustifyContentMode.BETWEEN);
            row.getStyle()
                    .set("padding", "8px 12px")
                    .set("border-left", "3px solid #FF9800")
                    .set("background-color", "#fafafa")
                    .set("border-radius", "4px")
                    .set("margin-bottom", "6px");

            container.add(row);
        }
        return container;
    }

    private boolean isStale(Person person, LocalDate today) {
        if (person.getLastContactedAt() == null) return true;
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                person.getLastContactedAt().toLocalDate(), today);
        return days >= STALE_DAYS;
    }
}