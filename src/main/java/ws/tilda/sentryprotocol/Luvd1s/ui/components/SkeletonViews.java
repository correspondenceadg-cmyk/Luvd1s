package ws.tilda.sentryprotocol.Luvd1s.ui.components;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public final class SkeletonViews {

    private SkeletonViews() {
    }

    public static VerticalLayout grid(int rows) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        container.setWidthFull();
        for (int i = 0; i < rows; i++) {
            Skeleton row = new Skeleton("100%", "40px");
            row.getStyle().set("margin-bottom", "6px");
            container.add(row);
        }
        return container;
    }

    public static VerticalLayout statCards(int count) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(true);
        container.setWidthFull();
        for (int i = 0; i < count; i++) {
            container.add(new Skeleton("100%", "70px"));
        }
        return container;
    }

    public static VerticalLayout barChart(int rows) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(true);
        container.setWidthFull();
        for (int i = 0; i < rows; i++) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setSpacing(true);
            Skeleton label = new Skeleton("100px", "20px");
            Skeleton bar = new Skeleton("100%", "20px");
            bar.setWidthFull();
            Skeleton value = new Skeleton("30px", "20px");
            row.add(label, bar, value);
            row.setFlexGrow(1, bar);
            container.add(row);
        }
        return container;
    }

    public static VerticalLayout cardList(int rows) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        container.setWidthFull();
        for (int i = 0; i < rows; i++) {
            VerticalLayout card = new VerticalLayout();
            card.setPadding(false);
            card.setSpacing(false);
            card.addClassName("skeleton-card");
            card.add(new Skeleton("30%", "16px"));
            card.add(new Skeleton("90%", "14px"));
            container.add(card);
        }
        return container;
    }
}