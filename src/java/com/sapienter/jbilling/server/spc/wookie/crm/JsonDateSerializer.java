package com.sapienter.jbilling.server.spc.wookie.crm;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

class JsonDateSerializer extends JsonSerializer<Date> {
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    @Override
    public void serialize(final Date date, final JsonGenerator gen, final SerializerProvider provider)
            throws IOException {
        gen.writeString(formatDate(date));
    }

    private String formatDate(final Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
        return simpleDateFormat.format(date);
    }

}
