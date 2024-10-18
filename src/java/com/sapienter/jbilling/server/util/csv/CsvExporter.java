/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.util.csv;

import au.com.bytecode.opencsv.CSVWriter;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.converter.BigDecimalConverter;
import com.sapienter.jbilling.server.util.converter.TimestampConverter;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * CsvExporter
 *
 * @author Brian Cowdery
 * @since 03/03/11
 */
public class CsvExporter<T extends Exportable> implements Exporter<T> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /** The maximum safe number of exportable elements to processes.  */
    public static final Integer MAX_RESULTS = 1000000;
    public static final Integer GENERATE_CSV_LIMIT = 50000;

    static {
        ConvertUtils.register(new BigDecimalConverter(), BigDecimal.class);
        ConvertUtils.register(new TimestampConverter(), Timestamp.class);
    }

    private Class<T> type;

    private CsvExporter(Class<T> type) {
        this.type = type;
    }

    /**
     * Factory method to produce a new instance of CsvExporter for the given type.
     *
     * @param type type of exporter
     * @param <T> type T
     * @return new exporter of type T
     */
    public static <T extends Exportable> CsvExporter<T> createExporter(Class<T> type) {
        return new CsvExporter<>(type);
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public String export(List<T> list) {
        String[] header;

        if(CollectionUtils.isNotEmpty(list)) {
            header = list.get(0).getFieldNames();
        } else {
            // list can be empty, instantiate a new instance of type to
            // extract the field names for the CSV header
            try {
                header = type.newInstance().getFieldNames();
            } catch (InstantiationException e) {
                logger.debug("Could not produce a new instance of {} to build CSV header.", type.getSimpleName());
                return null;

            } catch (IllegalAccessException e) {
                logger.debug("Constructor of {} is not accessible to build CSV header.", type.getSimpleName());
                return null;
            }
        }

        try (StringWriter out = new StringWriter()) {
            try(CSVWriter writer = new CSVWriter(out)) {
                writer.writeNext(header);
                for (Exportable exportable : list) {
                    for (Object[] values : exportable.getFieldValues()) {
                        writer.writeNext(convertToString(values));
                    }
                }
            }
            return out.toString();
        } catch (Exception e) {
            logger.error("CSV Export Failed!", e);
            throw new SessionInternalError(e);
        }

    }

    public String[] convertToString(Object[] objects) {
        String[] strings = new String[objects.length];
        int i = 0;
        for (Object object : objects) {
            if (object != null) {
                Converter converter = ConvertUtils.lookup(object.getClass());
				if (converter != null) {
				    strings[i++] = converter.convert(object.getClass(),object).toString();
				} else {
                    strings[i++] = object.toString();
                }
            } else {
                strings[i++] = "";
            }
        }
        return strings;
    }

}
