package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import com.googlecode.ez.vcard.VCard;
import com.googlecode.ez.vcard.VCardProperty;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ContactImporter {

    public List<Person> parse(String filename, InputStream in) throws IOException {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) return parseCsv(in, ',');
        if (lower.endsWith(".tsv")) return parseCsv(in, '\t');
        if (lower.endsWith(".vcf") || lower.endsWith(".vcard")) return parseVcf(in);
        throw new IllegalArgumentException("Unsupported file type: " + filename);
    }

    private List<Person> parseCsv(InputStream in, char delim) throws IOException {
        List<Person> out = new ArrayList<>();
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(r, CSVFormat.DEFAULT
                     .withDelimiter(delim)
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord row : parser) {
                Person p = new Person();
                p.setFirstName(get(row, "first", "firstname", "given"));
                p.setLastName(get(row, "last", "lastname", "surname", "family"));
                p.setEmail(get(row, "email", "e-mail"));
                p.setPhone(get(row, "phone", "telephone", "mobile"));
                p.setCompany(get(row, "company", "organization", "org"));
                p.setJobTitle(get(row, "title", "jobtitle", "role"));
                if (p.getFirstName() != null || p.getLastName() != null || p.getEmail() != null) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    private String get(CSVRecord row, String... keys) {
        for (String k : keys) {
            if (row.isMapped(k)) {
                String v = row.get(k);
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }

    private List<Person> parseVcf(InputStream in) throws IOException {
        List<Person> out = new ArrayList<>();
        VCard vcard = VCard.fromInputStream(in, StandardCharsets.UTF_8);
        addPerson(out, vcard);
        return out;
    }

    private void addPerson(List<Person> out, VCard card) {
        if (card == null) return;
        Person p = new Person();
        VCardProperty fn = card.getFirstProperty("FN");
        String full = fn != null ? fn.getRawValue() : null;

        VCardProperty n = card.getFirstProperty("N");
        if (n != null) {
            String[] parts = n.getRawValue().split(";", -1);
            if (parts.length > 0 && !parts[0].isBlank()) p.setLastName(parts[0]);
            if (parts.length > 1 && !parts[1].isBlank()) p.setFirstName(parts[1]);
        }
        if (p.getFirstName() == null && full != null) {
            int space = full.indexOf(' ');
            if (space > 0) {
                p.setFirstName(full.substring(0, space));
                p.setLastName(full.substring(space + 1));
            } else {
                p.setFirstName(full);
            }
        }

        VCardProperty email = card.getFirstProperty("EMAIL");
        if (email != null) p.setEmail(email.getRawValue());

        VCardProperty tel = card.getFirstProperty("TEL");
        if (tel != null) p.setPhone(tel.getRawValue());

        VCardProperty org = card.getFirstProperty("ORG");
        if (org != null) p.setCompany(org.getRawValue());

        VCardProperty title = card.getFirstProperty("TITLE");
        if (title != null) p.setJobTitle(title.getRawValue());

        if (p.getFirstName() != null || p.getLastName() != null) out.add(p);
    }
}
