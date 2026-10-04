package ws.tilda.sentryprotocol.Luvd1s.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LineChart extends Div {

    private static final int WIDTH = 700;
    private static final int HEIGHT = 240;
    private static final int PADDING_LEFT = 50;
    private static final int PADDING_RIGHT = 20;
    private static final int PADDING_TOP = 20;
    private static final int PADDING_BOTTOM = 40;

    public LineChart(Map<LocalDate, Long> data, String accentColor) {
        getStyle().set("width", "100%");
        getStyle().set("max-width", WIDTH + "px");

        if (data.isEmpty()) {
            setText("No data to show yet.");
            getStyle().set("color", "var(--lumo-secondary-text-color)");
            getStyle().set("font-style", "italic");
            getStyle().set("padding", "16px 0");
            return;
        }

        Element svg = new Element("svg");
        svg.setAttribute("viewBox", "0 0 " + WIDTH + " " + HEIGHT);
        svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
        svg.setAttribute("style", "width:100%;height:auto;display:block;");

        int chartW = WIDTH - PADDING_LEFT - PADDING_RIGHT;
        int chartH = HEIGHT - PADDING_TOP - PADDING_BOTTOM;
        int baseY = PADDING_TOP + chartH;

        long maxY = Math.max(1, data.values().stream().max(Long::compare).orElse(1L));
        List<Map.Entry<LocalDate, Long>> entries = new ArrayList<>(data.entrySet());
        int n = entries.size();

        int gridCount = 4;
        for (int i = 0; i <= gridCount; i++) {
            int y = PADDING_TOP + (chartH * i) / gridCount;
            Element line = new Element("line");
            line.setAttribute("x1", String.valueOf(PADDING_LEFT));
            line.setAttribute("y1", String.valueOf(y));
            line.setAttribute("x2", String.valueOf(PADDING_LEFT + chartW));
            line.setAttribute("y2", String.valueOf(y));
            line.setAttribute("stroke", "var(--lumo-contrast-10pct)");
            line.setAttribute("stroke-width", "1");
            svg.appendChild(line);
        }

        Element axisX = new Element("line");
        axisX.setAttribute("x1", String.valueOf(PADDING_LEFT));
        axisX.setAttribute("y1", String.valueOf(baseY));
        axisX.setAttribute("x2", String.valueOf(PADDING_LEFT + chartW));
        axisX.setAttribute("y2", String.valueOf(baseY));
        axisX.setAttribute("stroke", "var(--lumo-contrast-30pct)");
        axisX.setAttribute("stroke-width", "1");
        svg.appendChild(axisX);

        Element axisY = new Element("line");
        axisY.setAttribute("x1", String.valueOf(PADDING_LEFT));
        axisY.setAttribute("y1", String.valueOf(PADDING_TOP));
        axisY.setAttribute("x2", String.valueOf(PADDING_LEFT));
        axisY.setAttribute("y2", String.valueOf(baseY));
        axisY.setAttribute("stroke", "var(--lumo-contrast-30pct)");
        axisY.setAttribute("stroke-width", "1");
        svg.appendChild(axisY);

        for (int i = 0; i <= gridCount; i++) {
            int y = PADDING_TOP + (chartH * i) / gridCount;
            long value = maxY - (maxY * i) / gridCount;
            Element text = new Element("text");
            text.setAttribute("x", String.valueOf(PADDING_LEFT - 8));
            text.setAttribute("y", String.valueOf(y + 4));
            text.setAttribute("text-anchor", "end");
            text.setAttribute("font-size", "11");
            text.setAttribute("fill", "var(--lumo-secondary-text-color)");
            text.setText(String.valueOf(value));
            svg.appendChild(text);
        }

        StringBuilder pathD = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int x = PADDING_LEFT + (n <= 1 ? chartW / 2 : (i * chartW) / (n - 1));
            long v = entries.get(i).getValue();
            int y = baseY - (int) ((v * chartH) / maxY);
            if (i == 0) pathD.append("M ").append(x).append(" ").append(y);
            else pathD.append(" L ").append(x).append(" ").append(y);
        }

        Element path = new Element("path");
        path.setAttribute("d", pathD.toString());
        path.setAttribute("fill", "none");
        path.setAttribute("stroke", accentColor);
        path.setAttribute("stroke-width", "2");
        path.setAttribute("stroke-linejoin", "round");
        path.setAttribute("stroke-linecap", "round");
        svg.appendChild(path);

        for (int i = 0; i < n; i++) {
            int x = PADDING_LEFT + (n <= 1 ? chartW / 2 : (i * chartW) / (n - 1));
            long v = entries.get(i).getValue();
            int y = baseY - (int) ((v * chartH) / maxY);

            Element dot = new Element("circle");
            dot.setAttribute("cx", String.valueOf(x));
            dot.setAttribute("cy", String.valueOf(y));
            dot.setAttribute("r", "3");
            dot.setAttribute("fill", accentColor);
            Element title = new Element("title");
            title.setText(entries.get(i).getKey() + ": " + v);
            dot.appendChild(title);
            svg.appendChild(dot);
        }

        int labelStep = Math.max(1, n / 6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        for (int i = 0; i < n; i += labelStep) {
            int x = PADDING_LEFT + (n <= 1 ? chartW / 2 : (i * chartW) / (n - 1));
            Element text = new Element("text");
            text.setAttribute("x", String.valueOf(x));
            text.setAttribute("y", String.valueOf(baseY + 16));
            text.setAttribute("text-anchor", "middle");
            text.setAttribute("font-size", "11");
            text.setAttribute("fill", "var(--lumo-secondary-text-color)");
            text.setText(entries.get(i).getKey().format(fmt));
            svg.appendChild(text);
        }

        getElement().appendChild(svg);
    }
}