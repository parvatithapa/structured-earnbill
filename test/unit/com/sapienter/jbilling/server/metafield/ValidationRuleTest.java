package com.sapienter.jbilling.server.metafield;

import com.sapienter.jbilling.server.metafields.validation.EmailValidationRuleModel;
import junit.framework.TestCase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
     * Created by leandro on 23/05/17.
 */
public class ValidationRuleTest extends TestCase {

    private static Pattern EMAIL_PATTERN = EmailValidationRuleModel.getMatchingPattern();
    private static String[] validEmails = new String[] {
            "admin@jbilling.com",
            "system+admin@jbilling.com",
            "system.admin@jbilling.com",
            "system_admin@jbilling.com"
    };

    private static String[] invalidEmails = new String[] {
            "adminjbilling.com",
            "system+admin@jbilling",
            "system@admin@jbilling.com"
    };

    private Matcher matcher;

    public void testEmailValidationRuleWithValidEmails() {
        for (String s : validEmails) {
            matcher = EMAIL_PATTERN.matcher(s);
            if (!matcher.matches()) {
                fail(String.format("The email %s should match", s));
            }
        }
    }

    public void testEmailValidationRuleWithInvalidEmails() {
        for (String s : invalidEmails) {
            matcher = EMAIL_PATTERN.matcher(s);
            if (matcher.matches()) {
                fail(String.format("The email %s shouldn't match", s));
            }
        }
    }
}
