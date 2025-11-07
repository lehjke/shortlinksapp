package com.shortlinks.util;

import java.security.SecureRandom;

public class ShortLinkGenerator {
    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
            .toCharArray();
    private final SecureRandom random = new SecureRandom();

    public String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(ALPHABET.length);
            builder.append(ALPHABET[index]);
        }
        return builder.toString();
    }
}
