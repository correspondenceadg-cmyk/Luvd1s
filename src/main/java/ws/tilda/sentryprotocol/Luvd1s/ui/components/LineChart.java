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
    private static final int HEIGHT = 280;
    private static final int PAD_L = 50;
    private static final int PAD_R = 20;
    private static final int PAD_T = 25;
    private static final int PAD_B = 55;

    public LineChart(Map<LocalDate, Long> data, String accentColor) {
        setWidthFull();
        getStyle().set("max-width", WIDTH + "px");
        getStyle().set("aspect-ratio", WIDTH + " / " + HEIGHT);
        getStyle().set("position", "relative");
        getStyle().set("overflow", "visible");

        if (data.isEmpty()) {
            setText("No data to show yet.");
            getStyle().set("color", "var(--lumo-secondary-text-color)");
            getStyle().set("font-style", "italic");
            getStyle().set("padding", "16px 0");
            getStyle().set("aspect-ratio", "unset");
            return;
        }

        Element svg = new Element("svg");
        svg.setAttribute("viewBox", "0 0 " + WIDTH + " " + HEIGHT);
        svg.setAttribute("preserveAspectRatio", "xMidYMid meet");
        svg.setAttribute("style",
                "position:absolute;top:0;left:0;width:100%;height:100%;display:block;");

        int chartW = WIDTH - PAD_L - PAD_R;
        int chartH = HEIGHT - PAD_T - PAD_B;
        int baseY = PAD_T + chartH;

        long maxY = Math.max(1, data.values().stream().max(Long::compare).orElse(1L));

        List<Map.Entry<LocalDate, Long>> entries = new ArrayList<>(data.entrySet());
        int n = entries.size();
        int gridCount = 4;

        for (int i = 0; i <= gridCount; i++) {
            int y = PAD_T + (chartH * i) / gridCount;
            Element line = new Element("line");
            line.setAttribute("x1", String.valueOf(PAD_L));
            line.setAttribute("y1", String.valueOf(y));
            line.setAttribute("x2", String.valueOf(PAD_L + chartW));
            line.setAttribute("y2", String.valueOf(y));
            line.setAttribute("stroke", "var(--lumo-contrast-10pct)");
            line.setAttribute("stroke-width", "1");
            svg.appendChild(line);
        }

        for (int i = 0; i <= gridCount; i++) {
            int y = PAD_T + (chartH * i) / gridCount;
            double raw = maxY - ((double) maxY * i) / gridCount;
            if (raw < 0) raw = 0;
            String label = (raw == Math.floor(raw))
                    ? String.valueOf((long) raw)
                    : String.format("%.1f", raw);

            Element text = new Element("text");
            text.setAttribute("x", String.valueOf(PAD_L - 8));
            text.setAttribute("y", String.valueOf(y + 4));
            text.setAttribute("text-anchor", "end");
            text.setAttribute("font-size", "12");
            text.setAttribute("fill", "var(--lumo-secondary-text-color)");
            text.setText(label);
            svg.appendChild(text);
        }

        Element axisX = new Element("line");
        axisX.setAttribute("x1", String.valueOf(PAD_L));
        axisX.setAttribute("y1", String.valueOf(baseY));
        axisX.setAttribute("x2", String.valueOf(PAD_L + chartW));
        axisX.setAttribute("y2", String.valueOf(baseY));
        axisX.setAttribute("stroke", "var(--lumo-contrast-30pct)");
        axisX.setAttribute("stroke-width", "1");
        svg.appendChild(axisX);

        Element axisY = new Element("line");
        axisY.setAttribute("x1", String.valueOf(PAD_L));
        axisY.setAttribute("y1", String.valueOf(PAD_T));
        axisY.setAttribute("x2", String.valueOf(PAD_L));
        axisY.setAttribute("y2", String.valueOf(baseY));
        axisY.setAttribute("stroke", "var(--lumo-contrast-30pct)");
        axisY.setAttribute("stroke-width", "1");
        svg.appendChild(axisY);

        StringBuilder pathD = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int x = PAD_L + (n <= 1 ? chartW / 2 : (i * chartW) / (n - 1));
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
            int x = PAD_L + (n <= 1 ? chartW / 2 : (i * chartW) / (n - 1));
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

        int labelStep = Math.max(1, (n + 5) / 6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        for (int i = 0; i < n; i += labelStep) {
            int x = PAD_L + (n <= 1 ? chartW / 2 : (i * chartW) / (n - 1));
            Element text = new Element("text");
            text.setAttribute("x", String.valueOf(x));
            text.setAttribute("y", String.valueOf(baseY + 20));
            text.setAttribute("text-anchor", "middle");
            text.setAttribute("font-size", "12");
            text.setAttribute("fill", "var(--lumo-secondary-text-color)");
            text.setText(entries.get(i).getKey().format(fmt));
            svg.appendChild(text);
        }

        getElement().appendChild(svg);
    }
}