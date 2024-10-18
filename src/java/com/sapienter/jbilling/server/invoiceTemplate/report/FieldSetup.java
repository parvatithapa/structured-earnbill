package com.sapienter.jbilling.server.invoiceTemplate.report;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.*;
import com.sapienter.jbilling.server.invoiceTemplate.domain.TableLines;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.sapienter.jbilling.server.invoiceTemplate.report.FieldType.Field;
import static com.sapienter.jbilling.server.invoiceTemplate.report.FieldType.Variable;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

/**
 * @author elmot
 */
public class FieldSetup {

    public static final List<FieldSetup> COMMON_FIELDS = asList(
            new FieldSetup("REPORT_COUNT", "REPORT_COUNT", Variable, Number.class),
            new FieldSetup("PAGE_COUNT", "PAGE_COUNT", Variable, Number.class),
            new FieldSetup("COLUMN_COUNT", "COLUMN_COUNT", Variable, Number.class),
            new FieldSetup("GROUP_COUNT", "GROUP_COUNT", Variable, Number.class, GroupCountProcessor.class),
            new FieldSetup("ADD_GROUP_COUNT", "GROUP_COUNT", Variable, Number.class, AddGroupCountProcessor.class)
    );

    public static final Set<FieldSetup> INVOICE_LINES_FIELDS = collectFields(InvoiceLineEnvelope.class);
    public static final Set<FieldSetup> CDR_EVENTS_FIELDS = collectFields(CdrEnvelope.class);

    private static final String UL_MONEY = "UL_MONEY";
    private static final String UL_DATE = "UL_DATE";
    private static final String UL_PCNT = "UL_PCNT";
    private static final String UL_DEC = "UL_DEC";
    private static final String $_CS = "$CS";

    private String name;
    private String description;
    private FieldType type;
    private Class valueClass;
    private Class<? extends FieldProcessor> processor;

    public FieldSetup(String name, String description, FieldType type, Class valueClass) {
        this(name, description, type, valueClass, DefaultFieldProcessor.class);
    }

    public FieldSetup(String name, String description, Class valueClass, Class<? extends FieldProcessor> processor) {
        this(name, description, Field, valueClass, processor);
    }

    public FieldSetup(String name, String description, FieldType type, Class valueClass, Class<? extends FieldProcessor> processor) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.valueClass = valueClass;
        this.processor = processor;
    }

    private static Set<FieldSetup> collectFields(Class<?> clazz) {
        Set<FieldSetup> fields = new HashSet<FieldSetup>();
        for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
            Field a = f.getAnnotation(Field.class);
            if (a != null) {
                String name = f.getName();
                Class<?> valueClass = a.valueClass();
                if (Object.class.equals(valueClass)) {
                    valueClass = f.getType();
                }
                String description = a.description();
                if (description == null || description.isEmpty()) {
                    description = name;
                }
                FieldType type = a.type();
                Class<? extends FieldProcessor> processor = a.processor();
                fields.add(new FieldSetup(name, description, type, valueClass, processor));
            }
        }
        return unmodifiableSet(fields);
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description == null ? getName() : description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FieldType getType() {
        return type == null ? Field : type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public Class getValueClass() {
        return valueClass == null ? Object.class : valueClass;
    }

    public void setValueClass(Class valueClass) {
        this.valueClass = valueClass;
    }

    public Class<? extends FieldProcessor> getProcessor() {
        return processor == null ? DefaultFieldProcessor.class : processor;
    }

    public void setProcessor(Class<? extends FieldProcessor> processor) {
        this.processor = processor;
    }

    public JRDesignExpression createExpression(TableLines cl, String formatter, String locale) {
        JRDesignExpression exp = new JRDesignExpression();
        Class type = getValueClass();
        String expr = getProcessorInstance().expression(this, cl);
        if (Number.class.isAssignableFrom(type)) {
            if (UL_MONEY.equals(formatter)) {
                exp.setText("$P{" + FormatUtil.PARAMETER_NAME + "}.money(" + expr + ")");
            } else if (UL_PCNT.equals(formatter)) {
                exp.setText("$P{" + FormatUtil.PARAMETER_NAME + "}.pcnt(" + expr + ")");
            } else if (UL_DEC.equals(formatter)) {
                exp.setText("$P{" + FormatUtil.PARAMETER_NAME + "}.dec(" + expr + ")");
            } else {
                formatter = formatter.replace($_CS, "\" + $P{currency_symbol} + \"");
                String expresionText = "new java.text.DecimalFormat(\"";
                if (StringUtils.isNotEmpty(locale)) {
                    expresionText = expresionText + formatter + "\", new java.text.DecimalFormatSymbols(new java.util.Locale(\"" + locale + "\"))).format(" + expr + ")";
                } else {
                    expresionText = expresionText + formatter + "\").format(" + expr + ")";
                }
                exp.setText(expresionText);
            }
            exp.setValueClass(String.class);
        } else if (Date.class.isAssignableFrom(type)) {
            if (UL_DATE.equals(formatter)) {
                exp.setText("$P{" + FormatUtil.PARAMETER_NAME + "}.date(" + expr + ")");
            } else {
                exp.setText("(new java.text.SimpleDateFormat(\"" + formatter + "\").format(" + expr + "))");
            }
            exp.setValueClass(String.class);
        } else {
            exp.setText(expr);
            exp.setValueClass(type);
        }
        return exp;
    }

    public FieldProcessor getProcessorInstance() {
        try {
            return getProcessor().newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldSetup that = (FieldSetup) o;

        return !(description != null ? !description.equals(that.description) : that.description != null)
                && !(name != null ? !name.equals(that.name) : that.name != null)
                && !(valueClass != null ? !valueClass.equals(that.valueClass) : that.valueClass != null);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (valueClass != null ? valueClass.hashCode() : 0);
        return result;
    }

}
