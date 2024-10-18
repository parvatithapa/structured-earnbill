package com.sapienter.jbilling.server.util.csv;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Gerhard
 * @since 11/12/13
 */
public class ExportableMapTest extends TestCase {

    private static final Logger logger = LoggerFactory.getLogger(ExportableMapTest.class);

    public void testExportableMap() {
        List list = new ArrayList();
        Map row = new LinkedHashMap();
        row.put("a", "a1");
        row.put("b", "b1");
        row.put("c", "c1");
        list.add(row);
        row = new LinkedHashMap();
        row.put("a", "a2");
        row.put("b", "b2");
        list.add(row);

        CsvExporter<ExportableMap> exporter = CsvExporter.createExporter(ExportableMap.class);
        String content = exporter.export(Arrays.asList(new ExportableMap[] {new ExportableMap(list)}));
        logger.debug(content);

        assertTrue(content.startsWith("\"a\",\"b\",\"c\""));
        assertTrue(content.contains("\"a1\",\"b1\",\"c1\""));
        assertTrue(content.contains("\"a2\",\"b2\",\"\""));
    }
}
