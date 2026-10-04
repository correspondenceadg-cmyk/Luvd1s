package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.util.PartialDate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ContactImporter {

    private static final Logger log = LoggerFactory.getLogger(ContactImporter.class);

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    };

    public List<Person> parse(String filename, InputStream input) throws IOException {
        if (filename == null) filename = "";
        String lower = filename.toLowerCase(Locale.ROOT);

        if (lower.endsWith(".vcf") || lower.endsWith(".vcard")) {
            return parseVCard(input);
        }
        if (lower.endsWith(".csv") || lower.endsWith(".tsv")) {
            return parseCsv(input, lower.endsWith(".tsv"));
        }

        byte[] head = input.readNBytes(64);
        String probe = new String(head, StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);
        InputStream combined = new SequenceInputStream(
                new ByteArrayInputStream(head), input);

        if (probe.contains("BEGIN:VCARD")) {
            return parseVCard(combined);
        }
        return parseCsv(combined, false);
    }

    private List<Person> parseVCard(InputStream input) throws IOException {
        String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        List<VCard> cards = Ezvcard.parse(text).all();

        List<Person> people = new ArrayList<>();
        for (VCard card : cards) {
            try {
                Person p = new Person();

                if (card.getStructuredName() != null) {
                    String given = card.getStructuredName().getGiven();
                    String family = card.getStructuredName().getFamily();
                    p.setFirstName(given != null ? given : "");
                    p.setLastName(family != null ? family : "");
                }
                if (p.getFirstName() == null || p.getFirstName().isBlank()) {
                    String fn = card.getFormattedName() != null
                            ? card.getFormattedName().getValue()
                            : null;
                    if (fn != null) {
                        String[] parts = fn.trim().split("\\s+", 2);
                        p.setFirstName(parts[0]);
                        if (parts.length > 1) p.setLastName(parts[1]);
                    }
                }
                if (card.getEmails() != null && !card.getEmails().isEmpty()) {
                    p.setEmail(card.getEmails().get(0).getValue());
                }
                if (card.getTelephoneNumbers() != null && !card.getTelephoneNumbers().isEmpty()) {
                    p.setPhone(card.getTelephoneNumbers().get(0).getText());
                }
                if (card.getOrganization() != null) {
                    List<String> org = card.getOrganization().getValues();
                    if (org != null && !org.isEmpty()) {
                        p.setCompany(org.get(0));
                    }
                }
                if (card.getTitles() != null && !card.getTitles().isEmpty()) {
                    p.setJobTitle(card.getTitles().get(0).getValue());
                }
                if (card.getBirthday() != null && card.getBirthday().getDate() != null) {
                    LocalDate bd = toLocalDate(card.getBirthday().getDate());
                    if (bd != null) {
                        p.setBirthday(bd);
                    }
                }

                if (p.getFirstName() != null && !p.getFirstName().isBlank()) {
                    people.add(p);
                }
            } catch (Exception ex) {
                log.warn("Skipping malformed vCard entry: {}", ex.getMessage());
            }
        }
        return people;
    }

    /**
     * ez-vcard 0.12 returns Temporal from Birthday.getDate(). Concrete types
     * in practice are LocalDate (fully specified), PartialDate (year only,
     * or month/day only), or java.util.Date on the legacy path. Anything
     * else gets dropped rather than guessed at.
     */
    private LocalDate toLocalDate(Temporal t) {
        if (t instanceof LocalDate ld) {
            return ld;
        }
        if (t instanceof PartialDate pd) {
            try {
                return pd.toLocalDate();
            } catch (Exception ex) {
                return null;
            }
        }
        if (t instanceof Date legacy) {
            return legacy.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }
        return null;
    }

    private List<Person> parseCsv(InputStream input, boolean tabSeparated) throws IOException {
        CSVFormat format = (tabSeparated
                ? CSVFormat.DEFAULT.builder().setDelimiter('\t')
                : CSVFormat.DEFAULT.builder())
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = new CSVParser(
                new InputStreamReader(input, StandardCharsets.UTF_8), format)) {

            List<Person> people = new ArrayList<>();
            for (CSVRecord record : parser) {
                try {
                    Person p = new Person();
                    p.setFirstName(lookup(record, "first name", "firstname", "given name", "given"));
                    p.setLastName(lookup(record, "last name", "lastname", "family name", "family", "surname"));
                    p.setEmail(lookup(record, "email", "e-mail", "email address"));
                    p.setPhone(lookup(record, "phone", "phone number", "mobile", "telephone", "tel"));
                    p.setCompany(lookup(record, "company", "organization", "organisation", "org"));
                    p.setJobTitle(lookup(record, "job title", "title", "role", "position"));
                    p.setNotes(lookup(record, "notes", "note", "comment"));

                    String birthday = lookup(record, "birthday", "birthdate", "birthday (yyyy-mm-dd)");
                    if (birthday != null) {
                        p.setBirthday(parseDate(birthday));
                    }

                    if (p.getFirstName() != null && !p.getFirstName().isBlank()) {
                        people.add(p);
                    }
                } catch (Exception ex) {
                    log.warn("Skipping malformed CSV row: {}", ex.getMessage());
                }
            }
            return people;
        }
    }

    private String lookup(CSVRecord record, String... candidates) {
        Map<String, String> map = record.toMap();
        for (Map.Entry<String, String> e : map.entrySet()) {
            String key = e.getKey() == null
                    ? ""
                    : e.getKey().trim().toLowerCase(Locale.ROOT);
            for (String candidate : candidates) {
                if (key.equals(candidate)) {
                    String value = e.getValue();
                    return (value == null || value.isBlank()) ? null : value.trim();
                }
            }
        }
        return null;
    }

    private LocalDate parseDate(String raw) {
        String s = raw.trim();
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(s, f);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return null;
    }
}