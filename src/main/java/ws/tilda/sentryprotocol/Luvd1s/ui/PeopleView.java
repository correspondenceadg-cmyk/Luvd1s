package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.Tag;
import ws.tilda.sentryprotocol.Luvd1s.service.InteractionService;
import ws.tilda.sentryprotocol.Luvd1s.service.PersonService;
import ws.tilda.sentryprotocol.Luvd1s.service.TagService;
import ws.tilda.sentryprotocol.Luvd1s.ui.components.SkeletonViews;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Push
@PermitAll
@Route("")
@PageTitle("People")
@StyleSheet("context://styles/skeleton.css")
public class PeopleView extends VerticalLayout {

    private static final int MAX_VISIBLE_ROWS = 20;
    private static final int ROW_HEIGHT = 44;
    private static final int HEADER_HEIGHT = 48;
    private static final int BUFFER = 8;

    private final PersonService personService;
    private final InteractionService interactionService;
    private final TagService tagService;
    private final AuthenticationContext authContext;

    private final Grid<Person> grid = new Grid<>(Person.class, false);
    private final Binder<Person> binder = new Binder<>(Person.class);
    private final List<Person> cachedPeople = new ArrayList<>();
    private final VerticalLayout gridContainer = new VerticalLayout();

    private final TextField firstName = new TextField("First name");
    private final TextField lastName = new TextField("Last name");
    private final EmailField email = new EmailField("Email");
    private final TextField phone = new TextField("Phone");
    private final TextField company = new TextField("Company");
    private final TextField jobTitle = new TextField("Job title");
    private final DatePicker birthday = new DatePicker("Birthday");
    private final TextArea notes = new TextArea("Notes");
    private final MultiSelectComboBox<Tag> tagsField = new MultiSelectComboBox<>("Tags");

    private final ComboBox<Tag> filterTag = new ComboBox<>("Filter by tag");

    private final Button save = new Button("Save");
    private final Button delete = new Button("Delete");
    private final Button newPerson = new Button("New");
    private final Button logInteraction = new Button("Log interaction", VaadinIcon.BOLT.create());

    private Person current;

    public PeopleView(PersonService personService,
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

        configureGrid();
        configureForm();

        filterTag.setItemLabelGenerator(Tag::getName);
        filterTag.setClearButtonVisible(true);
        filterTag.setWidthFull();
        filterTag.addValueChangeListener(e -> applyFilter());

        gridContainer.setPadding(false);
        gridContainer.setSpacing(false);
        gridContainer.setWidthFull();
        gridContainer.add(SkeletonViews.grid(8));

        add(filterTag, gridContainer, new H3("Edit person"), buildFormPanel());

        edit(null);
        loadAsync();
    }

    private void loadAsync() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UI ui = UI.getCurrent();

        CompletableFuture.runAsync(() -> {
            SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                List<Person> people = personService.findAll();
                List<Tag> tags = tagService.findAll();

                ui.access(() -> {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    try {
                        cachedPeople.clear();
                        cachedPeople.addAll(people);

                        filterTag.setItems(tags);

                        gridContainer.removeAll();
                        gridContainer.add(grid);
                        applyFilter();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    private HorizontalLayout buildHeader() {
        H2 title = new H2("People");

        Button dashboardButton = new Button("Dashboard", VaadinIcon.CHART.create(),
                e -> UI.getCurrent().navigate(DashboardView.class));
        dashboardButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button logoutButton = new Button("Log out", e -> authContext.logout());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttons = new HorizontalLayout(dashboardButton, logoutButton);
        buttons.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout(title, buttons);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        return header;
    }

    private void configureGrid() {
        grid.addComponentColumn(person -> {
            Button open = new Button("Open", e ->
                    UI.getCurrent().navigate(PersonDetailView.class, person.getId()));
            open.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            return open;
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);

        grid.addColumn(Person::getFirstName).setHeader("First name").setAutoWidth(true);
        grid.addColumn(Person::getLastName).setHeader("Last name").setAutoWidth(true);
        grid.addColumn(Person::getEmail).setHeader("Email").setAutoWidth(true);
        grid.addColumn(Person::getPhone).setHeader("Phone").setAutoWidth(true);

        grid.addComponentColumn(this::tagChips).setHeader("Tags").setAutoWidth(true);

        grid.addComponentColumn(person -> {
            Button log = new Button(VaadinIcon.BOLT.create(), e ->
                    new InteractionDialog(interactionService, person, this::onInteractionSaved).open());
            log.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return log;
        }).setHeader("Log").setAutoWidth(true).setFlexGrow(0);

        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));
        grid.setWidthFull();
    }

    private HorizontalLayout tagChips(Person person) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(false);
        person.getTags().forEach(tag -> {
            Span chip = new Span(tag.getName());
            chip.getStyle()
                    .set("background-color", tag.getColor() != null ? tag.getColor() : "#888")
                    .set("color", "white")
                    .set("padding", "2px 8px")
                    .set("border-radius", "12px")
                    .set("font-size", "0.75em")
                    .set("margin-right", "4px");
            row.add(chip);
        });
        return row;
    }

    private void configureForm() {
        binder.forField(firstName)
                .asRequired("First name required")
                .bind(Person::getFirstName, Person::setFirstName);
        binder.forField(lastName)
                .asRequired("Last name required")
                .bind(Person::getLastName, Person::setLastName);
        binder.forField(email).bind(Person::getEmail, Person::setEmail);
        binder.forField(phone).bind(Person::getPhone, Person::setPhone);
        binder.forField(company).bind(Person::getCompany, Person::setCompany);
        binder.forField(jobTitle).bind(Person::getJobTitle, Person::setJobTitle);
        binder.forField(birthday).bind(Person::getBirthday, Person::setBirthday);
        binder.forField(notes).bind(Person::getNotes, Person::setNotes);

        tagsField.setItemLabelGenerator(Tag::getName);
        binder.forField(tagsField).bind(Person::getTags, Person::setTags);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> savePerson());
        delete.addClickListener(e -> deletePerson());
        newPerson.addClickListener(e -> edit(null));
        logInteraction.addClickListener(e -> {
            if (current != null && current.getId() != null) {
                new InteractionDialog(interactionService, current, this::onInteractionSaved).open();
            } else {
                Notification.show("Select a person first");
            }
        });
    }

    private VerticalLayout buildFormPanel() {
        FormLayout form = new FormLayout(
                firstName, lastName, email, phone,
                company, jobTitle, birthday, tagsField, notes
        );
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        HorizontalLayout actions = new HorizontalLayout(save, delete, newPerson);
        VerticalLayout panel = new VerticalLayout(form, actions, logInteraction);
        panel.setPadding(false);
        panel.setSpacing(true);
        panel.setWidthFull();
        return panel;
    }

    private void applyFilter() {
        Tag selected = filterTag.getValue();
        List<Person> visible = selected == null
                ? cachedPeople
                : cachedPeople.stream().filter(p -> p.getTags().contains(selected)).toList();
        grid.setItems(visible);
        updateGridHeight(visible.size());
        if (current != null && current.getId() != null) {
            grid.select(current);
        }
    }

    private void onInteractionSaved() {
        applyFilter();
    }

    private void updateGridHeight(int rowCount) {
        if (rowCount <= MAX_VISIBLE_ROWS) {
            grid.setAllRowsVisible(true);
            grid.setHeight(null);
        } else {
            grid.setAllRowsVisible(false);
            int height = HEADER_HEIGHT + (MAX_VISIBLE_ROWS * ROW_HEIGHT) + BUFFER;
            grid.setHeight(height + "px");
        }
    }

    private void edit(Person person) {
        if (person == null) {
            current = new Person();
            grid.deselectAll();
            delete.setEnabled(false);
            logInteraction.setEnabled(false);
        } else {
            current = person;
            delete.setEnabled(true);
            logInteraction.setEnabled(true);
        }
        binder.setBean(current);
    }

    private void savePerson() {
        if (!binder.validate().isOk()) return;

        Person saved = personService.save(current);

        int idx = -1;
        for (int i = 0; i < cachedPeople.size(); i++) {
            if (Objects.equals(cachedPeople.get(i).getId(), saved.getId())) {
                idx = i;
                break;
            }
        }

        if (idx >= 0) {
            cachedPeople.set(idx, saved);
        } else {
            cachedPeople.add(saved);
            cachedPeople.sort(Comparator
                    .comparing(Person::getLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                    .thenComparing(Person::getFirstName, Comparator.nullsLast(String::compareToIgnoreCase)));
        }

        applyFilter();
        Notification.show("Saved");
    }

    private void deletePerson() {
        if (current == null || current.getId() == null) return;

        personService.delete(current);
        cachedPeople.removeIf(p -> Objects.equals(p.getId(), current.getId()));
        edit(null);
        applyFilter();
        Notification.show("Deleted");
    }
}