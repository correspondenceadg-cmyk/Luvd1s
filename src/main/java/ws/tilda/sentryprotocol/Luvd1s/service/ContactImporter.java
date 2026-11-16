package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.StructuredName;
import ezvcard.property.Email;
import ezvcard.property.Telephone;
import ezvcard.property.Organization;
import ezvcard.property.Title;
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
        for (VCard card : Ezvcard.parse(in).all()) {
            Person p = new Person();
            StructuredName n = card.getStructuredName();
            if (n != null) {
                if (n.getFamily() != null) p.setLastName(n.getFamily());
                if (n.getGiven() != null) p.setFirstName(n.getGiven());
            }
            if (p.getFirstName() == null && card.getFormattedName() != null) {
                String full = card.getFormattedName().getValue();
                int space = full.indexOf(' ');
                if (space > 0) {
                    p.setFirstName(full.substring(0, space));
                    p.setLastName(full.substring(space + 1));
                } else {
                    p.setFirstName(full);
                }
            }

            if (!card.getEmails().isEmpty()) p.setEmail(card.getEmails().get(0).getValue());
            if (!card.getTelephoneNumbers().isEmpty()) p.setPhone(card.getTelephoneNumbers().get(0).getText());
            Organization org = card.getOrganization();
            if (org != null && !org.getValues().isEmpty()) p.setCompany(String.join(", ", org.getValues()));
            if (!card.getTitles().isEmpty()) p.setJobTitle(card.getTitles().get(0).getValue());

            if (p.getFirstName() != null || p.getLastName() != null || p.getEmail() != null) {
                out.add(p);
            }
        }
        return out;
    }
}
