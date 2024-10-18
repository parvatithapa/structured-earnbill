package com.sapienter.jbilling.server.audit.mapper;

import org.apache.commons.beanutils.converters.DateTimeConverter;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

/**
 * DateConverter
 *
 * @author Brian Cowdery
 * @since 18-12-2012
 */
public class DateConverter extends DateTimeConverter {

    public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss Z";
    public static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern(DATE_PATTERN);

    public DateConverter() {
        super(null);                // set default value to null
        setPattern(DATE_PATTERN);   // use a sane date pattern
    }

    @Override
    protected Class getDefaultType() {
        return Date.class;
    }
}
