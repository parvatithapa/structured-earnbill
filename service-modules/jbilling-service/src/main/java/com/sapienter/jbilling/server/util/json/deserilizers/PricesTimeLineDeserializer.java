package com.sapienter.jbilling.server.util.json.deserilizers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.*;

/**
 * @author Vojislav Stanojevikj
 * @since 27-Sep-2016.
 */
public class PricesTimeLineDeserializer extends JsonDeserializer<SortedMap<Date, PriceModelWS>> {

    @Override
    public SortedMap<Date, PriceModelWS> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

        SortedMap<Date, PriceModelWS> sortedPrices = new TreeMap<>();
        Map<String, PriceModelWS> map = p.readValueAs(new TypeReference<LinkedHashMap<String, PriceModelWS>>() {});
        for (Map.Entry<String, PriceModelWS> entry : map.entrySet()){
            String millisString = entry.getKey();
            if (StringUtils.isEmpty(millisString) || !StringUtils.isNumeric(millisString)){
                throw new SessionInternalError("Invalid milliseconds key!");
            }
            long millis = Long.parseLong(millisString);
            sortedPrices.put(new Date(millis), entry.getValue());
        }
        return sortedPrices;
    }
}
