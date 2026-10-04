package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strips personally identifying information from text before it leaves
 * the server for an external AI provider.
 *
 * This is intentionally aggressive. The AI does not need to know the
 * names, emails, phones, or addresses of contacts to help the user
 * reason about them.
 */
@Component
public class PiiScrubber {

    private static final Pattern EMAIL = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

    private static final Pattern PHONE = Pattern.compile(
            "(?<!\\d)(?:\\+?\\d{1,3}[\\s.-]?)?(?:\\(?\\d{2,4}\\)?[\\s.-]?){2,4}\\d{2,4}(?!\\d)");

    private static final Pattern URL = Pattern.compile(
            "https?://\\S+|\\bwww\\.\\S+\\b");

    private static final Pattern IP = Pattern.compile(
            "\\b\\d{1,3}(?:\\.\\d{1,3}){3}\\b");

    private static final Pattern HANDLE = Pattern.compile(
            "(?<![\\w/])@[A-Za-z][A-Za-z0-9_]{2,30}\\b");

    private static final Pattern LONG_DIGITS = Pattern.compile(
            "\\b\\d{5,}\\b");

    /**
     * Scrub text with no knowledge of who the subject is.
     */
    public String scrub(String text) {
        return scrub(text, (Person) null);
    }

    /**
     * Scrub text and additionally redact the given person's identifiers.
     * Use this when the AI is being asked about a specific contact.
     */
    public String scrub(String text, Person person) {
        if (text == null || text.isBlank()) return "";

        String result = text;

        // Order matters: URLs and emails first, so their contents don't
        // accidentally match other patterns.
        result = EMAIL.matcher(result).replaceAll("[redacted-email]");
        result = URL.matcher(result).replaceAll("[redacted-url]");
        result = IP.matcher(result).replaceAll("[redacted-ip]");

        if (person != null) {
            result = redactPerson(result, person);
        }

        result = PHONE.matcher(result).replaceAll("[redacted-phone]");
        result = HANDLE.matcher(result).replaceAll("[redacted-handle]");
        result = LONG_DIGITS.matcher(result).replaceAll("[redacted-number]");

        return result;
    }

    /**
     * Batch scrub: applies the same rules to a list of strings.
     */
    public List<String> scrubAll(List<String> lines) {
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(scrub(line));
        }
        return out;
    }

    private String redactPerson(String text, Person person) {
        List<String> needles = new ArrayList<>();

        if (person.getFirstName() != null && !person.getFirstName().isBlank()) {
            needles.add(person.getFirstName());
        }
        if (person.getLastName() != null && !person.getLastName().isBlank()) {
            needles.add(person.getLastName());
        }
        if (person.getFirstName() != null && person.getLastName() != null) {
            needles.add(person.getFirstName() + " " + person.getLastName());
            needles.add(person.getLastName() + ", " + person.getFirstName());
        }
        if (person.getEmail() != null && !person.getEmail().isBlank()) {
            needles.add(person.getEmail());
        }
        if (person.getPhone() != null && !person.getPhone().isBlank()) {
            needles.add(person.getPhone());
        }

        // Longest first so "Alice Nguyen" matches before "Alice".
        needles.sort((a, b) -> Integer.compare(b.length(), a.length()));

        String result = text;
        for (String needle : needles) {
            if (needle.isBlank()) continue;
            result = result.replaceAll(
                    "(?i)" + Pattern.quote(needle),
                    "[contact]");
        }
        return result;
    }
}