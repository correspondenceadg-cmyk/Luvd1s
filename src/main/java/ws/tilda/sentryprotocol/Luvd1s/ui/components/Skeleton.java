package ws.tilda.sentryprotocol.Luvd1s.ui.components;

import com.vaadin.flow.component.html.Div;

public class Skeleton extends Div {

    public Skeleton() {
        this("100%", "20px");
    }

    public Skeleton(String width, String height) {
        setWidth(width);
        setHeight(height);
        addClassName("skeleton");
    }
}