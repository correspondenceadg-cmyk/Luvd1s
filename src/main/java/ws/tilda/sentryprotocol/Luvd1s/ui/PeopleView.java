package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.Tag;
import ws.tilda.sentryprotocol.Luvd1s.service.AiChatService;
import ws.tilda.sentryprotocol.Luvd1s.service.ContactImporter;
import ws.tilda.sentryprotocol.Luvd1s.service.InteractionService;
import ws.tilda.sentryprotocol.Luvd1s.service.PersonService;
import ws.tilda.sentryprotocol.Luvd1s.service.TagService;
import ws.tilda.sentryprotocol.Luvd1s.ui.components.AiChatPanel;
import ws.tilda.sentryprotocol.Luvd1s.ui.components.Feedback;
import ws.tilda.sentryprotocol.Luvd1s.ui.components.SkeletonViews;
import ws.tilda.sentryprotocol.Luvd1s.ui.components.ThemeToggle;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@PermitAll
@Route("people")
@PageTitle("People")
public class PeopleView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(PeopleView.class);

    private static final int MAX_VISIBLE_ROWS = 20;
    private static final int ROW_HEIGHT = 44;
    private static final int HEADER_HEIGHT = 48;
    private static final int BUFFER = 8;

    private final PersonService personService;
    private final InteractionService interactionService;
    private final TagService tagService;
    private final AuthenticationContext authContext;
    private final AiChatService aiChatService;
    private final ContactImporter contactImporter;

    private final Grid<Person> grid = new Grid<>(Person.class, false);
    private final Binder<Person> binder = new Binder<>(Person.class);
    private final List<Person> cachedPeople = new ArrayList<>();
    private final VerticalLayout gridContainer = new VerticalLayout();
    private VerticalLayout formPanel;

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
    private final Button cancel = new Button("Cancel");
    private final Button delete = new Button("Delete");
    private final Button newPerson = new Button("New");
    private final Button logInteraction = new Button("Log interaction", VaadinIcon.BOLT.create());

    private Person current;
    private boolean dirty = false;

    public PeopleView(PersonService personService,
                      InteractionService interactionService,
                      TagService tagService,
                      AuthenticationContext authContext,
                      AiChatService aiChatService,
                      ContactImporter contactImporter) {
        this.personService = personService;
        this.interactionService = interactionService;
        this.tagService = tagService;
        this.authContext = authContext;
        this.aiChatService = aiChatService;
        this.contactImporter = contactImporter;

        setPadding(false);
        setSpacing(true);
        getStyle().set("padding", "16px");
        getStyle().set("box-sizing", "border-box");
        setWidthFull();

        add(buildHeader());

        configureGrid();
        configureForm();

        filterTag.setItemLabelGenerator(Tag::getName);
        filterTag.setClearButtonVisible(true);
        filterTag.setWidthFull();
        filterTag.setAriaLabel("Filter people by tag");
        filterTag.addValueChangeListener(e -> applyFilter());

        gridContainer.setPadding(false);
        gridContainer.setSpacing(false);
        gridContainer.setWidthFull();
        gridContainer.add(SkeletonViews.grid(8));

        formPanel = buildFormPanel();

        add(filterTag, gridContainer, new H3("Edit person"), formPanel);
        add(new AiChatPanel(aiChatService));

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
            } catch (Exception ex) {
                log.error("Failed to load people and tags", ex);
                ui.access(() -> Notification.show("Couldn't load data: " + ex.getMessage()));
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    private HorizontalLayout buildHeader() {
        H2 title = new H2("People");
        title.getStyle().set("margin", "0");

        Button dashboardButton = new Button("Dashboard", VaadinIcon.CHART.create(),
                e -> UI.getCurrent().navigate(DashboardView.class));
        dashboardButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dashboardButton.setAriaLabel("Go to dashboard");

        Button importButton = new Button("Import", VaadinIcon.UPLOAD.create(),
                e -> new ImportDialog(contactImporter, personService, this::loadAsync).open());
        importButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        importButton.setAriaLabel("Import contacts from a file");

        Button debugButton = new Button("Debug", VaadinIcon.COG.create(),
                e -> UI.getCurrent().navigate(DebugView.class));
        debugButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        debugButton.setAriaLabel("Open debug dashboard");

        ThemeToggle themeToggle = new ThemeToggle();

        Button logoutButton = new Button("Log out", e -> authContext.logout());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logoutButton.setAriaLabel("Log out of your account");

        HorizontalLayout buttons = new HorizontalLayout(
                dashboardButton, importButton, debugButton, themeToggle, logoutButton);
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

    private void configureGrid() {
        grid.addComponentColumn(person -> {
            Button open = new Button("Open", e ->
                    UI.getCurrent().navigate(PersonDetailView.class, person.getId()));
            open.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            open.setAriaLabel("Open " + person.getFirstName() + " " + person.getLastName());
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
            log.setAriaLabel("Log interaction with "
                    + person.getFirstName() + " " + person.getLastName());
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

        trackDirty(firstName);
        trackDirty(lastName);
        trackDirty(email);
        trackDirty(phone);
        trackDirty(company);
        trackDirty(jobTitle);
        trackDirty(birthday);
        trackDirty(notes);
        trackDirty(tagsField);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> savePerson());
        save.setAriaLabel("Save person");

        cancel.addClickListener(e -> attemptCancel());
        cancel.setAriaLabel("Cancel edits and discard changes");

        delete.addClickListener(e -> deletePerson());
        delete.setAriaLabel("Delete person");

        newPerson.addClickListener(e -> edit(null));
        newPerson.setAriaLabel("Create new person");

        logInteraction.addClickListener(e -> {
            if (current != null && current.getId() != null) {
                new InteractionDialog(interactionService, current, this::onInteractionSaved).open();
            } else {
                Notification.show("Select a person first");
            }
        });
        logInteraction.setAriaLabel("Log interaction with the currently edited person");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void trackDirty(HasValue field) {
        field.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                setDirty(true);
            }
        });
    }

    private void setDirty(boolean value) {
        if (this.dirty == value) return;
        this.dirty = value;
        save.setEnabled(value);
    }

    private VerticalLayout buildFormPanel() {
        FormLayout form = new FormLayout(
                firstName, lastName, email, phone,
                company, jobTitle, birthday, tagsField, notes
        );
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        HorizontalLayout actions = new HorizontalLayout(save, cancel, delete, newPerson);
        actions.getStyle().set("flex-wrap", "wrap");
        actions.getStyle().set("gap", "4px");

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
        Feedback.fadeRefresh(gridContainer);
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
        setDirty(false);
    }

    private void attemptCancel() {
        if (!dirty) return;

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Discard changes?");
        dialog.setText("Your unsaved edits will be lost.");
        dialog.setCancelable(true);
        dialog.setCancelText("Keep editing");
        dialog.setConfirmText("Discard");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> revert());
        dialog.open();
    }

    private void revert() {
        if (current == null) return;
        if (current.getId() == null) {
            edit(null);
        } else {
            personService.findById(current.getId()).ifPresent(this::edit);
        }
    }

    private void savePerson() {
        if (!binder.validate().isOk()) {
            Feedback.shake(formPanel);
            return;
        }

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

        edit(saved);
        applyFilter();

        Feedback.pulseSuccess(save);
        Feedback.fadeRefresh(gridContainer);
        Notification.show("Saved");
    }

    private void deletePerson() {
        if (current == null || current.getId() == null) return;

        personService.delete(current);
        cachedPeople.removeIf(p -> Objects.equals(p.getId(), current.getId()));
        edit(null);
        applyFilter();

        Feedback.fadeRefresh(gridContainer);
        Notification.show("Deleted");
    }
}