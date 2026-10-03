package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;
import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.service.InteractionService;
import ws.tilda.sentryprotocol.Luvd1s.service.PersonService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route("person")
@PageTitle("Person")
public class PersonDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final PersonService personService;
    private final InteractionService interactionService;

    private final VerticalLayout header = new VerticalLayout();
    private final VerticalLayout timeline = new VerticalLayout();

    private Person person;

    public PersonDetailView(PersonService personService,
                            InteractionService interactionService) {
        this.personService = personService;
        this.interactionService = interactionService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Button back = new Button("Back", VaadinIcon.ARROW_LEFT.create(),
                e -> UI.getCurrent().navigate(PeopleView.class));
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button log = new Button("Log interaction", VaadinIcon.BOLT.create());
        log.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        log.addClickListener(e -> {
            if (person != null) {
                new InteractionDialog(interactionService, person, this::render).open();
            }
        });

        HorizontalLayout actions = new HorizontalLayout(back, log);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.BETWEEN);

        add(actions, header, new H3("Interaction history"), timeline);
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        personService.findById(parameter).ifPresentOrElse(
                p -> { this.person = p; render(); },
                () -> add(new Span("Person not found"))
        );
    }

    private void render() {
        header.removeAll();

        if (person == null) return;

        H2 name = new H2(person.getFirstName() + " " + person.getLastName());
        header.add(name);

        StringBuilder metaLine = new StringBuilder();
        if (person.getJobTitle() != null) metaLine.append(person.getJobTitle());
        if (person.getCompany() != null) {
            if (metaLine.length() > 0) metaLine.append(" at ");
            metaLine.append(person.getCompany());
        }
        if (metaLine.length() > 0) {
            Span meta = new Span(metaLine.toString());
            meta.getStyle().set("color", "#666");
            header.add(meta);
        }

        if (person.getEmail() != null) {
            Span emailSpan = new Span(person.getEmail());
            emailSpan.getStyle().set("color", "#666").set("font-size", "0.9em");
            header.add(emailSpan);
        }

        header.setPadding(false);
        header.setSpacing(false);

        renderTimeline();
    }

    private void renderTimeline() {
        timeline.removeAll();
        List<Interaction> interactions = interactionService.findByPerson(person);

        if (interactions.isEmpty()) {
            Span empty = new Span("No interactions logged yet.");
            empty.getStyle().set("color", "#888").set("font-style", "italic");
            timeline.add(empty);
            return;
        }

        for (Interaction i : interactions) {
            timeline.add(interactionCard(i));
        }

        timeline.setPadding(false);
        timeline.setSpacing(false);
    }

    private Component interactionCard(Interaction interaction) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(false);

        Span typeBadge = new Span(interaction.getType().name());
        typeBadge.getStyle()
                .set("background-color", "#2196F3")
                .set("color", "white")
                .set("padding", "2px 10px")
                .set("border-radius", "12px")
                .set("font-size", "0.75em")
                .set("font-weight", "600");

        Span when = new Span(interaction.getOccurredAt()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm")));
        when.getStyle().set("color", "#666").set("font-size", "0.85em");

        HorizontalLayout cardHeader = new HorizontalLayout(typeBadge, when);
        cardHeader.setSpacing(true);
        cardHeader.setAlignItems(Alignment.CENTER);

        card.add(cardHeader);

        if (interaction.getSummary() != null && !interaction.getSummary().isBlank()) {
            Span summary = new Span(interaction.getSummary());
            summary.getStyle().set("margin-top", "6px");
            card.add(summary);
        }

        card.getStyle()
                .set("border-left", "3px solid #2196F3")
                .set("padding", "12px")
                .set("margin-bottom", "10px")
                .set("background-color", "#fafafa")
                .set("border-radius", "4px")
                .set("width", "100%");

        return card;
    }
}