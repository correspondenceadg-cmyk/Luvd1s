package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.service.ContactImporter;
import ws.tilda.sentryprotocol.Luvd1s.service.PersonService;
import ws.tilda.sentryprotocol.Luvd1s.ui.components.Feedback;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImportDialog extends Dialog {

    private final ContactImporter importer;
    private final PersonService personService;
    private final Runnable onImported;

    private final List<Person> parsed = new ArrayList<>();
    private final Grid<Person> preview = new Grid<>(Person.class, false);
    private final Span status = new Span();
    private final Button commit = new Button("Import");

    public ImportDialog(ContactImporter importer,
                        PersonService personService,
                        Runnable onImported) {
        this.importer = importer;
        this.personService = personService;
        this.onImported = onImported;

        setHeaderTitle("Import contacts");
        setWidth("640px");
        setMaxWidth("95vw");
        setHeight("640px");
        setMaxHeight("85vh");

        Upload upload = new Upload(event -> {
            try (InputStream in = event.getInputStream()) {
                List<Person> list = importer.parse(event.getFileName(), in);
                parsed.clear();
                parsed.addAll(list);
                preview.setItems(parsed);
                status.setText(list.size() + " contact" + (list.size() == 1 ? "" : "s")
                        + " ready to import.");
                commit.setEnabled(!list.isEmpty());
            } catch (Exception ex) {
                status.setText("Couldn't parse the file: " + ex.getMessage());
                commit.setEnabled(false);
            }
        });
        upload.setAcceptedFileTypes(".csv", ".tsv", ".vcf", ".vcard");
        upload.setMaxFiles(1);
        upload.setDropLabel(new Span("Drop a .csv, .tsv, or .vcf file"));
        upload.setWidthFull();

        configurePreview();

        status.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "0.9em")
                .set("display", "block")
                .set("margin", "12px 0 8px 0");

        VerticalLayout body = new VerticalLayout(
                upload,
                status,
                new H3("Preview"),
                preview
        );
        body.setPadding(false);
        body.setSpacing(true);
        body.setWidthFull();
        body.setHeightFull();
        body.setFlexGrow(1, preview);
        add(body);

        commit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        commit.setEnabled(false);
        commit.addClickListener(e -> doImport());

        Button cancel = new Button("Cancel", e -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        getFooter().add(cancel, commit);
    }

    private void configurePreview() {
        preview.addColumn(Person::getFirstName).setHeader("First").setAutoWidth(true);
        preview.addColumn(Person::getLastName).setHeader("Last").setAutoWidth(true);
        preview.addColumn(Person::getEmail).setHeader("Email").setAutoWidth(true);
        preview.addColumn(Person::getCompany).setHeader("Company").setAutoWidth(true);
        preview.setWidthFull();
        preview.setHeightFull();
    }

    private void doImport() {
        if (parsed.isEmpty()) return;

        int success = 0;
        int failed = 0;
        for (Person p : parsed) {
            try {
                personService.save(p);
                success++;
            } catch (Exception ex) {
                failed++;
            }
        }

        Feedback.pulseSuccess(commit);
        Notification.show("Imported " + success + " contact"
                + (success == 1 ? "" : "s")
                + (failed > 0 ? " (" + failed + " failed)" : ""));

        onImported.run();
        close();
    }
}