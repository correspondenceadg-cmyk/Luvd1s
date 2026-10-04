package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;
import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.service.InteractionService;
import ws.tilda.sentryprotocol.Luvd1s.service.PersonService;
import ws.tilda.sentryprotocol.Luvd1s.ui.components.ThemeToggle;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

import java.time.format.DateTimeFormatter;
import java.util.List;

@PermitAll
@Route("person")
@PageTitle("Person")
public class PersonDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final PersonService personService;
    private final InteractionService interactionService;
    private final AuthenticationContext authContext;

    private final VerticalLayout header = new VerticalLayout();
    private final VerticalLayout timeline = new VerticalLayout();

    private Person person;

    public PersonDetailView(PersonService personService,
                            InteractionService interactionService,
                            AuthenticationContext authContext) {
        this.personService = personService;
        this.interactionService = interactionService;
        this.authContext = authContext;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        personService.findById(parameter).ifPresentOrElse(
                p -> {
                    this.person = p;
                    render();
                },
                () -> {
                    removeAll();
                    Button back = new Button("Back", VaadinIcon.ARROW_LEFT.create(),
                            e -> UI.getCurrent().navigate(PeopleView.class));
                    back.setAriaLabel("Back to people list");
                    add(back, new Span("Person not found"));
                }
        );
    }

    private void render() {
        removeAll();

        Button back = new Button("Back", VaadinIcon.ARROW_LEFT.create(),
                e -> UI.getCurrent().navigate(PeopleView.class));
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.setAriaLabel("Back to people list");

        Button log = new Button("Log interaction", VaadinIcon.BOLT.create());
        log.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        log.addClickListener(e -> new InteractionDialog(
                interactionService, person, this::render).open());
        log.setAriaLabel("Log interaction with " + person.getFirstName() + " " + person.getLastName());

        ThemeToggle themeToggle = new ThemeToggle();

        Button logout = new Button("Log out", e -> authContext.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logout.setAriaLabel("Log out of your account");

        HorizontalLayout actions = new HorizontalLayout(back, log, themeToggle, logout);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.BETWEEN);
        actions.setAlignItems(Alignment.CENTER);

        header.removeAll();
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
            meta.getStyle().set("color", "var(--lumo-secondary-text-color)");
            header.add(meta);
        }
        if (person.getEmail() != null) {
            Span emailSpan = new Span(person.getEmail());
            emailSpan.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "0.9em");
            header.add(emailSpan);
        }
        header.setPadding(false);
        header.setSpacing(false);

        add(actions, header, new H3("Interaction history"), timeline);
        renderTimeline();
    }

    private void renderTimeline() {
        timeline.removeAll();
        List<Interaction> interactions = interactionService.findByPerson(person);

        if (interactions.isEmpty()) {
            Span empty = new Span("No interactions logged yet.");
            empty.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-style", "italic");
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
        when.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.85em");

        Button edit = new Button(VaadinIcon.EDIT.create(), e ->
                new InteractionDialog(interactionService, person, this::render, interaction).open());
        edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        edit.setAriaLabel("Edit this interaction");

        Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(interaction));
        delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY,
                ButtonVariant.LUMO_ERROR);
        delete.setAriaLabel("Delete this interaction");

        HorizontalLayout meta = new HorizontalLayout(typeBadge, when);
        meta.setSpacing(true);
        meta.setAlignItems(Alignment.CENTER);
        meta.setFlexGrow(1);

        HorizontalLayout actions = new HorizontalLayout(edit, delete);
        actions.setSpacing(false);

        HorizontalLayout cardHeader = new HorizontalLayout(meta, actions);
        cardHeader.setWidthFull();
        cardHeader.setAlignItems(Alignment.CENTER);
        cardHeader.setJustifyContentMode(JustifyContentMode.BETWEEN);

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
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "4px")
                .set("width", "100%");

        return card;
    }

    private void confirmDelete(Interaction interaction) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete this interaction?");
        dialog.setText("This can't be undone.");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            interactionService.delete(interaction);
            render();
            Notification.show("Interaction deleted");
        });
        dialog.open();
    }
}