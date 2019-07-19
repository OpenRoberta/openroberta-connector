package de.fhg.iais.roberta.util;

import java.util.Random;

/**
 * Class for generating tokens.
 *
 * @author dpyka
 */
public final class OraTokenGenerator {
    private static final String ALPHABET = "123456789ABCDEFGHIJKLMNPQRSTUVWXYZ";
    private static final int ALPHABET_LENGTH = ALPHABET.length();
    private static final int TOKEN_LENGTH = 8;

    private OraTokenGenerator() {
    }

    /**
     * Create a new token as String of 8 characters length.
     *
     * @return The token on which the brick is being linked to a client.
     */
    public static String generateToken() {
        StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
        Random random = new Random();
        for ( int i = 0; i < 8; i++ ) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET_LENGTH)));
        }
        return sb.toString();
    }
}
