package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;
import ws.tilda.sentryprotocol.Luvd1s.data.InteractionType;
import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.service.InteractionService;
import ws.tilda.sentryprotocol.Luvd1s.ui.components.Feedback;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextArea;

import java.time.LocalDateTime;

public class InteractionDialog extends Dialog {

    private final InteractionService service;
    private final Person person;
    private final Runnable onSaved;
    private final Interaction existing;

    private final ComboBox<InteractionType> type = new ComboBox<>("Type");
    private final DateTimePicker occurredAt = new DateTimePicker("When");
    private final TextArea summary = new TextArea("Summary");

    private final FormLayout form = new FormLayout();

    public InteractionDialog(InteractionService service, Person person, Runnable onSaved) {
        this(service, person, onSaved, null);
    }

    public InteractionDialog(InteractionService service,
                             Person person,
                             Runnable onSaved,
                             Interaction existing) {
        this.service = service;
        this.person = person;
        this.onSaved = onSaved;
        this.existing = existing;

        setHeaderTitle(existing == null
                ? "Log interaction with " + person.getFirstName()
                : "Edit interaction");
        setWidth("500px");

        type.setItems(InteractionType.values());
        summary.setWidthFull();

        if (existing != null) {
            type.setValue(existing.getType());
            occurredAt.setValue(existing.getOccurredAt());
            summary.setValue(existing.getSummary());
        } else {
            type.setValue(InteractionType.CALL);
            occurredAt.setValue(LocalDateTime.now());
        }

        form.add(type, occurredAt, summary);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(form);

        Button saveButton = new Button(existing == null ? "Save" : "Update", e -> save(saveButtonOrSelf()));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> close());

        getFooter().add(cancelButton, saveButton);
    }

    private Button saveButtonOrSelf() {
        return (Button) getFooter().getComponentAt(1);
    }

    private void save(Button saveButton) {
        if (type.getValue() == null || occurredAt.getValue() == null) {
            Feedback.shake(form);
            Notification.show("Type and date are required");
            return;
        }

        Interaction interaction = existing != null ? existing : new Interaction();
        interaction.setPerson(person);
        interaction.setType(type.getValue());
        interaction.setOccurredAt(occurredAt.getValue());
        interaction.setSummary(summary.getValue());

        service.save(interaction);

        Feedback.pulseSuccess(saveButton);
        Notification.show(existing == null ? "Interaction logged" : "Interaction updated");
        onSaved.run();
        close();
    }
}