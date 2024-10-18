package com.sapienter.jbilling.server.invoiceTemplate.servlet;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author elmot
 */
public class JsonData {
    public static final String PROPERTY_NAME = "json";

    public static String getTestJson(ServletContext context) throws IOException {
        String json = (String) context.getAttribute(PROPERTY_NAME);
        if (json == null) {
            json = readDefaultJson();
            saveTestJson(context, json);
        }
        return json;
    }

    public static void saveTestJson(ServletContext context, String json) {
        context.setAttribute(PROPERTY_NAME, json);
    }

    private static String readDefaultJson() throws IOException {
        StringBuilder res = new StringBuilder();
        InputStream stream = JsonData.class.getResourceAsStream("PdfTest.json");
        for (int c; (c = stream.read()) >= 0; ) {
            res.append((char) c);
        }
        return res.toString();
    }
}
