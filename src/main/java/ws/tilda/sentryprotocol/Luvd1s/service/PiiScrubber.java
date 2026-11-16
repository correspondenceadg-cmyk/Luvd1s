package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PiiScrubber {

    private static final Pattern EMAIL =
            Pattern.compile("\\b[\\w.+-]+@[\\w-]+\\.[\\w.-]+\\b");
    private static final Pattern PHONE =
            Pattern.compile("\\+?\\d[\\d\\s().-]{6,}\\d");
    private static final Pattern URL =
            Pattern.compile("https?://\\S+");
    private static final Pattern IP =
            Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern HANDLE =
            Pattern.compile("(?<![\\w/])@[A-Za-z0-9_]{2,}");

    // scrub("mail alice@x.com, +1 312 315 5249") -> "mail [redacted-email], [redacted-phone]"
    public String scrub(String input) {
        if (input == null || input.isBlank()) return input;
        String s = input;
        s = EMAIL.matcher(s).replaceAll("[redacted-email]");
        s = URL.matcher(s).replaceAll("[redacted-url]");
        s = IP.matcher(s).replaceAll("[redacted-ip]");
        s = HANDLE.matcher(s).replaceAll("[redacted-handle]");
        s = PHONE.matcher(s).replaceAll("[redacted-phone]");
        return s;
    }

    // scrub("Alice called", Alice) -> "[redacted-name] called"
    public String scrub(String input, Person person) {
        String s = scrub(input);
        if (person == null) return s;
        if (person.getFirstName() != null && !person.getFirstName().isBlank()) {
            s = s.replace(person.getFirstName(), "[redacted-name]");
        }
        if (person.getLastName() != null && !person.getLastName().isBlank()) {
            s = s.replace(person.getLastName(), "[redacted-name]");
        }
        return s;
    }
}
