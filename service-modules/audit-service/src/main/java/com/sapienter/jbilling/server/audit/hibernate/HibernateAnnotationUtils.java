package com.sapienter.jbilling.server.audit.hibernate;

import javax.persistence.Column;
import javax.persistence.Table;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilities for processing javax.persistence and Hibernate annotations.
 *
 * @author Brian Cowdery
 * @since 16-12-2012
 */
public class HibernateAnnotationUtils {

    private static final Map<Class, HBaseEntityModel> HIBERNATE_MODEL_CACHE = new HashMap<>();

    public static HBaseEntityModel getHibernateEntityModel(Class type) {
        if (!HIBERNATE_MODEL_CACHE.containsKey(type)) {
            HIBERNATE_MODEL_CACHE.put(type, buildEntityModel(type));
        }

        return HIBERNATE_MODEL_CACHE.get(type);
    }

    // todo: This was quick and dirty... try and update the annotation scanning code to use the Reflections library

    /**
     * Builds a model of the entity objects using the javax.persistence annotations on
     * the entity class.
     *
     * @param type entity type
     * @return entity model.
     */
    @SuppressWarnings("unchecked")
    public static HBaseEntityModel buildEntityModel(Class type) {
        HBaseEntityModel model = new HBaseEntityModel();

        // class level annotations
        Annotation annotation = type.getAnnotation(Table.class);
        if (annotation != null && annotation instanceof Table) {
            Table table = (Table) annotation;
            if (table.name() != null && !table.name().isEmpty()) {
                model.setTableName(table.name());
            }

        }

        /*
            For beans persisted with Hibernate, go over the getters/setters that were annotated to collect
            columns. There are also some classes that are annotated at the field level
         */

        // collect public member methods of the class, including those defined on the interface
        // or those inherited from a super class or super interface.
        for (Method method : type.getMethods()) {
            Column column = method.getAnnotation(Column.class);
            if (column != null) {
                if (method.getName().startsWith("get") || method.getName().startsWith("set")) {
                    model.getColumnFields().put(getFieldName(method.getName()), column.name());
                }
            }
        }

        // collection all field annotations, including private fields that
        // we can to access via a public accessor method
        Class klass = type;
        while (klass != null) {
            for (Field field : klass.getDeclaredFields()) {
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    model.getColumnFields().put(field.getName(), column.name());
                }
            }

            // try the super class
            klass = klass.getSuperclass();
        }

        return model;
    }

    /**
     * Converts a method name to a javabeans field name.
     *
     * @param methodName method name
     * @return field name
     */
    private static String getFieldName(String methodName) {
        return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
    }
}
