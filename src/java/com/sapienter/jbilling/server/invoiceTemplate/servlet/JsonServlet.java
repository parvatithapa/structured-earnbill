package com.sapienter.jbilling.server.invoiceTemplate.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author elmot
 */
public class JsonServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        String value = JsonData.getTestJson(getServletContext());
        resp.getWriter().write(value);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = req.getReader();
        for (String s; (s = reader.readLine()) != null; ) {
            builder.append(s);
        }
        JsonData.saveTestJson(getServletContext(), builder.toString());
        resp.getWriter().write("OK");
    }
}
