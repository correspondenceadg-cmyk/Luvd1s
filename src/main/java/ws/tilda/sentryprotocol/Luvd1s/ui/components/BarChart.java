package ws.tilda.sentryprotocol.Luvd1s.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BarChart extends Div {

    private static final int WIDTH = 700;
    private static final int HEIGHT = 280;
    private static final int PAD_L = 50;
    private static final int PAD_R = 20;
    private static final int PAD_T = 25;
    private static final int PAD_B = 55;
    private static final int GAP_PCT = 20;
    private static final double MAX_BAR_WIDTH = 90.0;

    public BarChart(Map<String, Long> data, String color) {
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
        svg.setAttribute("preserveAspectRatio", "xMidYMid meet");
        svg.setAttribute("style",
                "display:block;width:100%;height:auto;max-width:" + WIDTH + "px;");

        int chartW = WIDTH - PAD_L - PAD_R;
        int chartH = HEIGHT - PAD_T - PAD_B;
        int baseY = PAD_T + chartH;

        long maxY = Math.max(1, data.values().stream().max(Long::compare).orElse(1L));

        int gridCount = 4;

        // Horizontal gridlines
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

        // Y-axis labels — double math so labels are distinct
        for (int i = 0; i <= gridCount; i++) {
            int y = PAD_T + (chartH * i) / gridCount;
            double raw = maxY - ((double) maxY * i) / gridCount;
            long value = Math.round(raw);
            if (value < 0) value = 0;

            Element text = new Element("text");
            text.setAttribute("x", String.valueOf(PAD_L - 8));
            text.setAttribute("y", String.valueOf(y + 4));
            text.setAttribute("text-anchor", "end");
            text.setAttribute("font-size", "12");
            text.setAttribute("fill", "var(--lumo-secondary-text-color)");
            text.setText(String.valueOf(value));
            svg.appendChild(text);
        }

        // Axes
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

        List<Map.Entry<String, Long>> entries = new ArrayList<>(data.entrySet());
        int n = entries.size();
        double slotW = (double) chartW / n;
        double barW = Math.min(slotW * (1 - GAP_PCT / 100.0), MAX_BAR_WIDTH);

        for (int i = 0; i < n; i++) {
            Map.Entry<String, Long> entry = entries.get(i);
            long v = entry.getValue();
            int barH = (int) ((v * chartH) / maxY);
            double x = PAD_L + i * slotW + (slotW - barW) / 2.0;
            int y = baseY - barH;

            Element bar = new Element("rect");
            bar.setAttribute("x", String.valueOf(x));
            bar.setAttribute("y", String.valueOf(y));
            bar.setAttribute("width", String.valueOf(barW));
            bar.setAttribute("height", String.valueOf(Math.max(barH, 1)));
            bar.setAttribute("rx", "3");
            bar.setAttribute("fill", color);
            Element title = new Element("title");
            title.setText(entry.getKey() + ": " + v);
            bar.appendChild(title);
            svg.appendChild(bar);

            Element valueText = new Element("text");
            valueText.setAttribute("x", String.valueOf(x + barW / 2.0));
            valueText.setAttribute("y", String.valueOf(y - 6));
            valueText.setAttribute("text-anchor", "middle");
            valueText.setAttribute("font-size", "12");
            valueText.setAttribute("font-weight", "600");
            valueText.setAttribute("fill", "var(--lumo-body-text-color)");
            valueText.setText(String.valueOf(v));
            svg.appendChild(valueText);

            Element labelText = new Element("text");
            labelText.setAttribute("x", String.valueOf(x + barW / 2.0));
            labelText.setAttribute("y", String.valueOf(baseY + 20));
            labelText.setAttribute("text-anchor", "middle");
            labelText.setAttribute("font-size", "12");
            labelText.setAttribute("fill", "var(--lumo-secondary-text-color)");
            labelText.setText(entry.getKey());
            svg.appendChild(labelText);
        }

        getElement().appendChild(svg);
    }
}