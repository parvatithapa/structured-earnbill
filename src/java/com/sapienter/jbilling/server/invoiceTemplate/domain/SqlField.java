package com.sapienter.jbilling.server.invoiceTemplate.domain;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by aman on 22/1/15.
 */
public class SqlField {
    String name;
    String expr;
    String kind = "SQLField";

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(SqlField.class));
    public static final Pattern JR_PARAMETER_PATTERN = Pattern.compile("\\$P\\{[A-z0-9_]+\\}");

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpr() {
        return expr;
    }

    public void setExpr(String expr) {
        this.expr = expr;
    }

    private void replaceParameters(Map parameters) throws Exception {
        // Find the $P{} in the expression. Replace them with the values from the map.
        // if no parameter found then throw error.
        Matcher matcher = JR_PARAMETER_PATTERN.matcher(expr);
        while (matcher.find()) {
            String parameterExpression = matcher.group();
            String parameterName = parameterExpression.substring(3, parameterExpression.length() - 1);
            if (parameters.containsKey(parameterName)) {
                Object parameterValue = parameters.get(parameterName);
                if (parameterValue != null && !parameterValue.toString().isEmpty()) {
                    expr = expr.replace(parameterExpression, parameterValue.toString());
                } else {
                    LOG.debug("parameter value not found for parameter : " + parameterName);
                    throw new IllegalArgumentException("Value not found for parameter : " + parameterName);
                }
            } else {
                LOG.debug("parameter used in query expression is not found in parameter list : " + parameterName);
                throw new IllegalArgumentException("Parameter used in query expression is not found : " + parameterName);
            }
        }
    }

    public Object setAsParameter(Map parameters) throws Exception {
        // This method will check that the name given for this SQL field is not used by any existing parameters
        LOG.debug("name of the SQL field : " + name);
        if (parameters.containsKey(name)) {
            throw new IllegalArgumentException("Name used for Sql Field is already used by other parameter : " + name);
        }
        // Replace Parameters used in query with their value
        replaceParameters(parameters);
        // execute the query
        LOG.debug("query executed : " + expr);
        Object output = executeQuery();
        LOG.debug("Output : " + output);
        // set into parameters
        return output;
    }

    private Object executeQuery() {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        Object output = null;
        try {
            SqlRowSet rs = jdbcTemplate.queryForRowSet(expr);
            // fetch only the single value
            if (rs.next()) {
                output = rs.getObject(1);
            }
        } catch (Exception exception) {
            LOG.error(exception);
            throw new RuntimeException(exception);
        }
        return output;
    }

    private JdbcTemplate getJdbcTemplate() {
        return Context.getBean(Context.Name.JDBC_TEMPLATE);
    }
}
