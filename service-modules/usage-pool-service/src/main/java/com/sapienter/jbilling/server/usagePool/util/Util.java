package com.sapienter.jbilling.server.usagePool.util;

import java.math.BigDecimal;

/**
 * Created by marcolin on 30/10/15.
 */
public class Util {
    public static BigDecimal string2decimal(String number) {
        if (number == null || number.isEmpty())
            return null;
        try {
            return new BigDecimal(number);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
