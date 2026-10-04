package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("register")
@PageTitle("Register")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    private final UserService userService;

    private final TextField username = new TextField("Username");
    private final TextField displayName = new TextField("Display name");
    private final PasswordField password = new PasswordField("Password");
    private final PasswordField confirm = new PasswordField("Confirm password");

    public RegisterView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        username.setRequired(true);
        password.setRequired(true);
        confirm.setRequired(true);

        FormLayout form = new FormLayout(username, displayName, password, confirm);
        form.setWidth("400px");
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button create = new Button("Create account", e -> createAccount());
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button back = new Button("Back to login", e ->
                UI.getCurrent().navigate(LoginView.class));
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        VerticalLayout wrapper = new VerticalLayout(form, create, back);
        wrapper.setAlignItems(Alignment.CENTER);
        wrapper.setPadding(false);

        add(new H1("Create your account"), wrapper);
    }

    private void createAccount() {
        if (username.isEmpty() || password.isEmpty()) {
            Notification.show("Username and password required");
            return;
        }
        if (!password.getValue().equals(confirm.getValue())) {
            Notification.show("Passwords don't match");
            return;
        }
        if (password.getValue().length() < 6) {
            Notification.show("Password must be at least 6 characters");
            return;
        }

        try {
            userService.register(
                    username.getValue(),
                    password.getValue(),
                    displayName.isEmpty() ? username.getValue() : displayName.getValue()
            );
            Notification.show("Account created — please log in");
            UI.getCurrent().navigate(LoginView.class);
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage());
        }
    }
}