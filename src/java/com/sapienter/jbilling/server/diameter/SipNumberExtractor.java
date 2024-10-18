package com.sapienter.jbilling.server.diameter;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This utility class extracts and normalizes phone numbers from
 * SIP URIs.
 */
public class SipNumberExtractor {

    /**
     * Extracts the phone number part from a well-formed SIP URI string.
     *
     * @param sipUri a SIP URI
     * @return the phone number part extracted, or <code>null</code> if the passed URI is null or
     *         not a valid SIP URI, or the phone number is not global (starts with a plus sign).
     */
    public static String extract (String sipUri) {
        if (sipUri == null) {
            return null;
        }

        String result = null;

        boolean number = false;
        try {
            String trimmed = sipUri.trim();

            if (trimmed.matches("[a-z]+:.*")) {
                trimmed = trimmed.substring(trimmed.indexOf(":") + 1);
                if (trimmed.length() > 0) {
                    if (trimmed.startsWith("+")) {
                        //return number
                        result = copyUntil(trimmed, "@");
                        number = true;
                    } else {
                        if (trimmed.contains("@")) {
                            //return email
                            //remove password
                            if(trimmed.contains(":") && (trimmed.indexOf(":") < trimmed.indexOf("@"))){
                                trimmed = trimmed.substring(0, trimmed.indexOf(":")) + trimmed.substring(trimmed.indexOf("@"), trimmed.length());
                            }
                            result = trimmed;
                        }
                    }
                    if(result != null){
                        result = copyUntil(result, "?");
                        result = copyUntil(result, ":");
                        result = copyUntil(result, ";");
                        if(number){
                            result = result.replaceAll("\\D+", "");
                        }
                    }
                }
            }

        } catch (IndexOutOfBoundsException e) {
            result = null;
        }


        return result;
    }

    private static String copyUntil(String source, String character){
        String out = source;
        if (source.contains(character)) {
            out = source.substring(0, source.indexOf(character));
        }
        return out;
    }
}
