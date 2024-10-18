package com.sapienter.jbilling.server.util.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sapienter.jbilling.server.pricing.PriceModelWS;

import java.io.IOException;
import java.util.Date;
import java.util.SortedMap;

/**
 * @author Vojislav Stanojevikj
 * @since 27-Sep-2016.
 */
public class PriceTimeLineSerializer extends JsonSerializer<SortedMap<Date, PriceModelWS>> {

    @Override
    public void serialize(SortedMap<Date, PriceModelWS> value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {

        gen.writeStartObject();
        if (null != value && !value.isEmpty()){
            for (SortedMap.Entry<Date, PriceModelWS> entry : value.entrySet()){
                long millis = entry.getKey().getTime();
                gen.writeObjectField(String.valueOf(millis), entry.getValue());
            }
        }
        gen.writeEndObject();
    }
}
