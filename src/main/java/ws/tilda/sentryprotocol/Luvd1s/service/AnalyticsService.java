package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;
import ws.tilda.sentryprotocol.Luvd1s.data.InteractionType;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AnalyticsService {

    public Map<LocalDate, Long> interactionsByWeek(List<Interaction> interactions, int weeks) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusWeeks(weeks - 1).with(DayOfWeek.MONDAY);

        Map<LocalDate, Long> buckets = new TreeMap<>();
        for (LocalDate d = start; !d.isAfter(today); d = d.plusWeeks(1)) {
            buckets.put(d, 0L);
        }

        for (Interaction i : interactions) {
            if (i.getOccurredAt() == null) continue;
            LocalDate d = i.getOccurredAt().toLocalDate();
            if (d.isBefore(start)) continue;
            LocalDate weekStart = d.with(DayOfWeek.MONDAY);
            buckets.merge(weekStart, 1L, Long::sum);
        }
        return buckets;
    }

    public Map<String, Long> countByType(List<Interaction> interactions) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (InteractionType type : InteractionType.values()) {
            counts.put(type.name(), 0L);
        }
        for (Interaction i : interactions) {
            if (i.getType() != null) {
                counts.merge(i.getType().name(), 1L, Long::sum);
            }
        }
        counts.entrySet().removeIf(e -> e.getValue() == 0);
        return counts;
    }
}