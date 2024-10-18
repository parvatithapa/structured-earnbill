package com.sapienter.jbilling.server.invoiceTemplate.ui;

import com.google.gson.*;
import com.sapienter.jbilling.server.invoiceTemplate.domain.*;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author elmot
 */
public class JsonFactory {

    private static final Map<String, Class<?>> CONSTRUCTOR_MAP;
    public static final String KIND = "kind";

    private JsonFactory() {
    }

    private static final GsonBuilder BUILDER;

    static {
        BUILDER = new GsonBuilder();
        DocElementAdapter typeAdapter = new DocElementAdapter();
        BUILDER.setPrettyPrinting().registerTypeAdapter(DocElement.class, typeAdapter);
        BUILDER.setPrettyPrinting().registerTypeAdapter(CommonLines.class, typeAdapter);
        ClassAdapter classAdapter = new ClassAdapter();
        BUILDER.setPrettyPrinting().registerTypeAdapter(Class.class, classAdapter);
        CONSTRUCTOR_MAP = new ConcurrentHashMap<String, Class<?>>();
        addConstructor(TextBox.class);
        addConstructor(Image.class);
        addConstructor(InvoiceLines.class);
        addConstructor(EventLines.class);
        addConstructor(Section.class);
        addConstructor(SubReport.class);
        addConstructor(List.class);
        addConstructor(Text.class);
    }

    private static void addConstructor(Class<?> aClass) {
        CONSTRUCTOR_MAP.put(aClass.getSimpleName(), aClass);
    }

    public static Gson getGson() {
        return BUILDER.create();
    }

    private static class DocElementAdapter implements JsonSerializer<Object>, JsonDeserializer<Object> {

        @Override
        public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
            String kind = src.getClass().getSimpleName();
            JsonElement elem = context.serialize(src);
            elem.getAsJsonObject().addProperty(KIND, kind);
            return elem;
        }

        @Override
        public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            JsonPrimitive prim = (JsonPrimitive) jsonObject.get(KIND);
            Class<?> clazz = CONSTRUCTOR_MAP.get(prim.getAsString());
            return context.deserialize(jsonObject, clazz);
        }
    }

    private static class ClassAdapter implements JsonSerializer<Class>, JsonDeserializer<Class> {

        @Override
        public JsonElement serialize(Class src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getCanonicalName());
        }

        @Override
        public Class deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return Class.forName(json.getAsString());
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }

}
