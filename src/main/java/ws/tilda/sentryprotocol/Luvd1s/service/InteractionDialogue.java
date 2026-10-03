package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;
import ws.tilda.sentryprotocol.Luvd1s.data.InteractionType;
import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.service.InteractionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.time.LocalDateTime;

public class InteractionDialog extends Dialog {

    private final InteractionService service;
    private final Person person;
    private final Runnable onSaved;

    private final ComboBox<InteractionType> type = new ComboBox<>("Type");
    private final DateTimePicker occurredAt = new DateTimePicker("When");
    private final TextArea summary = new TextArea("Summary");

    public InteractionDialog(InteractionService service, Person person, Runnable onSaved) {
        this.service = service;
        this.person = person;
        this.onSaved = onSaved;

        setHeaderTitle("Log interaction with " + person.getFirstName());
        setWidth("500px");

        type.setItems(InteractionType.values());
        type.setValue(InteractionType.CALL);
        occurredAt.setValue(LocalDateTime.now());
        summary.setWidthFull();

        FormLayout form = new FormLayout(type, occurredAt, summary);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(form);

        Button save = new Button("Save", e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> close());

        getFooter().add(cancel, save);
    }

    private void save() {
        if (type.getValue() == null || occurredAt.getValue() == null) {
            Notification.show("Type and date are required");
            return;
        }

        Interaction interaction = new Interaction();
        interaction.setPerson(person);
        interaction.setType(type.getValue());
        interaction.setOccurredAt(occurredAt.getValue());
        interaction.setSummary(summary.getValue());

        service.log(interaction);
        Notification.show("Interaction logged");
        onSaved.run();
        close();
    }
}