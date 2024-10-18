package com.sapienter.jbilling.server.audit.mapper;

import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * StringConverter
 *
 * @author Brian Cowdery
 * @since 18-12-2012
 */
public class StringConverter implements Converter {

    private static final Logger LOG = Logger.getLogger(StringConverter.class);

    @Override
    public Object convert(Class aClass, Object o) {
        if (o == null)
            return null;

        if (o instanceof String) {
            if (Map.class.isAssignableFrom(aClass)) {
                return convertToMap((String) o);
            } else if (aClass.isEnum()) {
                return Enum.valueOf(aClass, (String) o);
            }
            return o;
        }


        if (o instanceof Date) {
            try {
                return DateConverter.FORMATTER.print(((Date) o).getTime());
            } catch (Throwable t) {
                return null;
            }
        }

        return o.toString();
    }

    private Map convertToMap(String value) {
        value = StringUtils.substringBetween(value, "{", "}");
        Map<String,String> map = null;
        if (!value.isEmpty()) {
            String[] keyValuePairs = value.split(",");
            try {
                map = new HashMap<>();
                if (keyValuePairs.length > 0)
                    for(String pair : keyValuePairs) {
                        String[] entry = pair.split("=");
                        map.put(entry[0].trim(), entry[1].trim());
                    }
                return map;
            } catch (Exception e) {
                LOG.debug("Exception while converting the value " + value + " in a map instance ", e);
            }
        }
        return map;
    }
}
