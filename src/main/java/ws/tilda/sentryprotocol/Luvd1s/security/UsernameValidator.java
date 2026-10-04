package ws.tilda.sentryprotocol.Luvd1s.security;

import java.text.Normalizer;

public final class UsernameValidator {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;

    private UsernameValidator() {
    }

    /**
     * Trims, normalizes to NFC, validates length, and rejects invisible/unsafe
     * Unicode categories. Returns the canonical form that should be stored.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Username required");
        }

        String s = raw.strip();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Username required");
        }

        s = Normalizer.normalize(s, Normalizer.Form.NFC);

        int codePointCount = s.codePointCount(0, s.length());
        if (codePointCount < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "Username must be at least " + MIN_LENGTH + " characters");
        }
        if (codePointCount > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Username must be at most " + MAX_LENGTH + " characters");
        }

        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            int type = Character.getType(cp);

            if (type == Character.CONTROL
                    || type == Character.FORMAT
                    || type == Character.SURROGATE
                    || type == Character.UNASSIGNED
                    || type == Character.PRIVATE_USE
                    || type == Character.LINE_SEPARATOR
                    || type == Character.PARAGRAPH_SEPARATOR) {
                throw new IllegalArgumentException(
                        "Username contains unsupported characters");
            }

            i += Character.charCount(cp);
        }

        return s;
    }
}