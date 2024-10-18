package com.sapienter.jbilling.rest;

import java.util.Random;

public class RestTestUtils {

    final private static String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    final private static int    LENGTH_LETTERS = LETTERS.length();

    final private static String DIGITS = "1234567890";
    final private static int    LENGTH_DIGITS = DIGITS.length();
    /**
     * Get random string
     *
     * @param len
     * @return
     */
    public static final String getRandomString(final int len) {
        final Random rnd = new Random();
        final StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(LETTERS.charAt(rnd.nextInt(LENGTH_LETTERS)));
        }
        return sb.toString();
    }

    /**
     * Get random number
     *
     * @param len
     * @return
     */
    public static final String getRandomNumber(final int len) {
        final Random rnd = new Random();
        final StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(DIGITS.charAt(rnd.nextInt(LENGTH_DIGITS)));
        }
        return sb.toString();
    }
}
