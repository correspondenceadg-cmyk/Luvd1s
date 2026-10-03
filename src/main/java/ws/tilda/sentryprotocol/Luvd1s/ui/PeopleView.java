package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.Tag;
import ws.tilda.sentryprotocol.Luvd1s.service.InteractionService;
import ws.tilda.sentryprotocol.Luvd1s.service.PersonService;
import ws.tilda.sentryprotocol.Luvd1s.service.TagService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route("")
@PageTitle("People")
public class PeopleView extends VerticalLayout {

    private final PersonService personService;
    private final InteractionService interactionService;
    private final TagService tagService;

    private final Grid<Person> grid = new Grid<>(Person.class, false);
    private final Binder<Person> binder = new Binder<>(Person.class);

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
                      TagService tagService) {
        this.personService = personService;
        this.interactionService = interactionService;
        this.tagService = tagService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("People"));

        configureGrid();
        configureForm();

        filterTag.setItems(tagService.findAll());
        filterTag.setItemLabelGenerator(Tag::getName);
        filterTag.setClearButtonVisible(true);
        filterTag.setWidthFull();
        filterTag.addValueChangeListener(e -> refresh());

        grid.setWidthFull();
        grid.setHeight("400px");

        add(filterTag, grid, new H3("Edit person"), buildFormPanel());

        refresh();
        edit(null);
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
                    new InteractionDialog(interactionService, person, this::refresh).open());
            log.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return log;
        }).setHeader("Log").setAutoWidth(true).setFlexGrow(0);

        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));
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

        tagsField.setItems(tagService.findAll());
        tagsField.setItemLabelGenerator(Tag::getName);
        binder.forField(tagsField).bind(Person::getTags, Person::setTags);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> savePerson());
        delete.addClickListener(e -> deletePerson());
        newPerson.addClickListener(e -> edit(null));
        logInteraction.addClickListener(e -> {
            if (current != null && current.getId() != null) {
                new InteractionDialog(interactionService, current, this::refresh).open();
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

    private void refresh() {
        List<Person> all = personService.findAll();
        Tag selected = filterTag.getValue();
        if (selected != null) {
            all = all.stream()
                    .filter(p -> p.getTags().contains(selected))
                    .toList();
        }
        grid.setItems(all);
        if (current != null && current.getId() != null) {
            grid.select(current);
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
        if (binder.validate().isOk()) {
            personService.save(current);
            refresh();
            Notification.show("Saved");
        }
    }

    private void deletePerson() {
        if (current == null || current.getId() == null) return;
        personService.delete(current);
        edit(null);
        refresh();
        Notification.show("Deleted");
    }
}