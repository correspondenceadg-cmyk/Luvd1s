package ws.tilda.sentryprotocol.Luvd1s.ui.components;

import ws.tilda.sentryprotocol.Luvd1s.service.AiChatService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.function.Supplier;

public class AiChatPanel extends Div {

    private final AiChatService chatService;

    private final Button fab;
    private final Div bubble;
    private final Dialog dialog;
    private final VerticalLayout messages;
    private final Scroller messagesScroller;
    private final TextField input;
    private final Div suggestions;
    private final Span quotaLabel;

    public AiChatPanel(AiChatService chatService) {
        this.chatService = chatService;

        fab = buildFab();
        bubble = buildBubble();
        dialog = new Dialog();
        messages = new VerticalLayout();
        messagesScroller = new Scroller(messages);
        input = new TextField();
        suggestions = new Div();
        quotaLabel = new Span();

        buildDialog();

        addClassName("ai-chat-panel");
        add(fab, bubble, dialog);
    }

    private Button buildFab() {
        Button button = new Button(VaadinIcon.CHAT.create());
        button.addClassName("ai-fab");
        button.setAriaLabel("Open AI assistant");
        button.setTooltipText("AI assistant");
        button.addClickListener(e -> openChat());
        return button;
    }

    private Div buildBubble() {
        Div b = new Div();
        b.addClassName("ai-bubble");
        b.add(new Span("Hey, do you need my help?"));
        b.addClickListener(e -> openChat());
        return b;
    }

    private void buildDialog() {
        dialog.setHeaderTitle("Assistant");
        dialog.addClassName("ai-dialog");
        dialog.setWidth("420px");
        dialog.setMaxWidth("95vw");
        dialog.setHeight("560px");
        dialog.setMaxHeight("85vh");
        dialog.setCloseOnEsc(true);

        suggestions.addClassName("ai-suggestions");
        suggestions.add(
                suggestionChip("Who should I reach out to?", chatService::whoShouldIReachOutTo),
                suggestionChip("Summarise my recent activity", chatService::summarizeRecentActivity)
        );

        quotaLabel.addClassName("ai-quota");
        updateQuotaLabel();

        messages.setPadding(false);
        messages.setSpacing(true);
        messages.setWidthFull();
        messages.addClassName("ai-messages");
        messagesScroller.setWidthFull();
        messagesScroller.setHeightFull();
        messagesScroller.getStyle().set("flex", "1");
        messagesScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);

        input.setPlaceholder("Ask anything…");
        input.setWidthFull();
        input.addClassName("ai-input");
        input.addKeyDownListener(com.vaadin.flow.component.Key.ENTER, e -> send());

        Button send = new Button(VaadinIcon.PAPERPLANE.create(), e -> send());
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        send.setAriaLabel("Send message");

        HorizontalLayout inputRow = new HorizontalLayout(input, send);
        inputRow.setWidthFull();
        inputRow.setAlignItems(Alignment.CENTER);
        inputRow.setSpacing(true);
        inputRow.getStyle().set("flex-shrink", "0");
        inputRow.setFlexGrow(1, input);

        VerticalLayout content = new VerticalLayout(suggestions, quotaLabel, messagesScroller, inputRow);
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();
        content.setHeightFull();
        content.setFlexGrow(1, messagesScroller);

        dialog.add(content);

        Button close = new Button("Close", e -> dialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(close);

        dialog.addOpenedChangeListener(e -> {
            if (e.isOpened()) {
                updateQuotaLabel();
                if (messages.getComponentCount() == 0) {
                    appendAssistant(
                            "Hi. I'm off by default and only see what you share with me. " +
                            "Names, emails, and phone numbers are stripped before anything is sent out. " +
                            "Try a suggestion above or ask a question."
                    );
                }
            }
        });
    }

    private void updateQuotaLabel() {
        int remaining = chatService.remainingRequests();
        int max = chatService.maxRequests();
        quotaLabel.setText(remaining + " of " + max + " requests remaining this hour");

        if (remaining == 0) {
            quotaLabel.getStyle().set("color", "#F44336");
            quotaLabel.getStyle().set("font-weight", "600");
        } else if (remaining <= 3) {
            quotaLabel.getStyle().set("color", "#FF9800");
            quotaLabel.getStyle().set("font-weight", "500");
        } else {
            quotaLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
            quotaLabel.getStyle().set("font-weight", "400");
        }
    }

    private Button suggestionChip(String label, Supplier<String> action) {
        Button chip = new Button(label);
        chip.addClassName("ai-chip");
        chip.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        chip.addClickListener(e -> {
            appendUser(label);
            chip.setEnabled(false);
            UI ui = UI.getCurrent();
            ui.setPollInterval(200);
            ui.getPage().executeJs("return new Promise(resolve => setTimeout(resolve, 40));")
                    .then(v -> {
                        String reply = action.get();
                        appendAssistant(reply);
                        updateQuotaLabel();
                        chip.setEnabled(true);
                        ui.setPollInterval(-1);
                    });
        });
        return chip;
    }

    private void openChat() {
        chatService.enableForSession();
        bubble.setVisible(false);
        getElement().executeJs("sessionStorage.setItem('luvd1s.ai.dismissed','true');");
        if (!dialog.isOpened()) {
            dialog.open();
        }
    }

    private void send() {
        String text = input.getValue();
        if (text == null || text.isBlank()) return;
        input.clear();
        appendUser(text);

        UI ui = UI.getCurrent();
        ui.setPollInterval(200);
        ui.getPage().executeJs("return new Promise(resolve => setTimeout(resolve, 40));")
                .then(v -> {
                    String reply = chatService.freeChat(text);
                    appendAssistant(reply);
                    updateQuotaLabel();
                    ui.setPollInterval(-1);
                });
    }

    private void appendUser(String text) {
        Div bubble = new Div();
        bubble.addClassName("ai-msg");
        bubble.addClassName("ai-msg-user");
        bubble.add(new Span(text));
        messages.add(bubble);
        scrollToBottom();
    }

    private void appendAssistant(String text) {
        Div bubble = new Div();
        bubble.addClassName("ai-msg");
        bubble.addClassName("ai-msg-assistant");
        bubble.add(new Span(text != null ? text : ""));
        messages.add(bubble);
        scrollToBottom();
    }

    private void scrollToBottom() {
        messagesScroller.getElement().executeJs(
                "requestAnimationFrame(() => { this.scrollTop = this.scrollHeight; });"
        );
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        getElement().executeJs(
                "return sessionStorage.getItem('luvd1s.ai.dismissed') === 'true';"
        ).then(Boolean.class, dismissed -> {
            if (Boolean.TRUE.equals(dismissed)) {
                bubble.setVisible(false);
            }
        });
    }
}