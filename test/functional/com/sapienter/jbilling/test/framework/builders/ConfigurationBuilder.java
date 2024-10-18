package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.RouteWS;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.PreferenceTypeWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEntityType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by marcomanzicore on 26/11/15.
 */
public class ConfigurationBuilder extends AbstractBuilder {

    private static int processingOrderForTest = 200;
    private List<MetaFieldWS> metafields = new ArrayList<>();
    private List<EnumerationWS> enumerations = new ArrayList<>();
    private HashMap<String, PluggableTaskWS> pluggableTaskWSes = new HashMap<>();
    private HashMap<String, String> routeNameFilePath = new HashMap<>();

    private ConfigurationBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        super(api, testEnvironment);
    }

    public static ConfigurationBuilder getBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        return new ConfigurationBuilder(api, testEnvironment);
    }

    public ConfigurationBuilder deletePlugin(String pluginClassName, Integer entityId){
        if (pluggableTaskWSes.containsKey(pluginClassName)){
            PluggableTaskWS pluggableTask = pluggableTaskWSes.remove(pluginClassName);
            api.deletePlugin(pluggableTask.getId());
        } else {
            PluggableTaskWS[] pluggableTasks = api.getPluginsWS(entityId, pluginClassName);
            if (null != pluggableTasks){
                for (PluggableTaskWS pluggableTask : pluggableTasks){
                    api.deletePlugin(pluggableTask.getId());
                }
            }
        }
        return this;
    }

    public boolean pluginExists(String className, Integer entityId) {
        boolean foundInLocalEnv = null != pluggableTaskWSes && pluggableTaskWSes.containsKey(className);
        return foundInLocalEnv || api.getPluginsWS(entityId, className).length != 0;
    }

    public ConfigurationBuilder addPlugin(String pluginClassName) {
        pluggableTaskWSes.put(pluginClassName, createPluggableTask(pluginClassName));
        return this;
    }

    public void withProcessingOrder(String pluginClassName, int processingOrder) {
        if (pluggableTaskWSes.containsKey(pluginClassName)){
            PluggableTaskWS pluggableTask = pluggableTaskWSes.get(pluginClassName);
            pluggableTask.setProcessingOrder(processingOrder);
            pluggableTaskWSes.put(pluginClassName, pluggableTask);
        }
    }

    public ConfigurationBuilder addPluginWithParameters(String pluginClassName, Hashtable<String, String> parameters) {
        PluggableTaskWS pluggableTaskWS = createPluggableTask(pluginClassName);
        pluggableTaskWS.setParameters(parameters);
        pluggableTaskWSes.put(pluginClassName, pluggableTaskWS);
        return this;
    }

    private PluggableTaskWS createPluggableTask(String pluginClassName) {
        PluggableTaskWS pluggableTaskWS = new PluggableTaskWS();
        pluggableTaskWS.setTypeId(api.getPluginTypeWSByClassName(pluginClassName).getId());
        pluggableTaskWS.setProcessingOrder(processingOrderForTest++);
        pluggableTaskWS.setNotes("");
        pluggableTaskWS.setOwningEntityId(api.getCallerCompanyId());
        return pluggableTaskWS;
    }

    public ConfigurationBuilder addEnumeration(String enumerationName, String... values) {
        EnumerationWS enumeration = new EnumerationWS();
        enumeration.setName(enumerationName);
        enumeration.setValues(Arrays.asList(values).stream().map(value -> new EnumerationValueWS(value)).collect(Collectors.toList()));
        enumerations.add(enumeration);
        return this;
    }

    public ConfigurationBuilder addMetaField(String metaFieldName, DataType dataType, EntityType type) {
        MetaFieldWS newMetaField = new MetaFieldWS();
        newMetaField.setName(metaFieldName);
        newMetaField.setDataType(dataType);
        newMetaField.setEntityType(type);
        newMetaField.setPrimary(true);
        newMetaField.setEntityId(api.getCallerCompanyId());
        metafields.add(newMetaField);
        return this;
    }

    public ConfigurationBuilder addRoute(String routeName, String filePathInFolder) {
        routeNameFilePath.put(routeName, filePathInFolder);
        return this;
    }

    public void build() {
        enumerations.forEach((enumeration) ->
                testEnvironment.add(enumeration.getName(), api.createUpdateEnumeration(enumeration), enumeration.getName(),
                        api, TestEntityType.ENUMERATION));
        pluggableTaskWSes.forEach((pluginClassName, pluginWS) ->
                testEnvironment.add(pluginClassName, api.createPlugin(pluginWS), pluginClassName, api,
                        TestEntityType.PLUGGABLE_TASK));
        metafields.forEach((metaField) ->
                testEnvironment.add(metaField.getName(), api.createMetaField(metaField), metaField.getName(), api,
                        TestEntityType.META_FIELD));
        routeNameFilePath.forEach((routeName, filePath) ->
                testEnvironment.add(routeName, createRoute(api, routeName, filePath),
                        routeName, api, TestEntityType.ROUTE));
    }

    private Integer createRoute(JbillingAPI api, String routeName, String filePath) {
        RouteWS routeWS = new RouteWS();
        routeWS.setName(routeName);
        routeWS.setEntityId(api.getCallerCompanyId());
        routeWS.setOutputFieldName("");
        routeWS.setDefaultRoute("");
        routeWS.setRootTable(false);
        routeWS.setRouteTable(false);
        routeWS.setTableName(null);
        return api.createRoute(routeWS, new File(filePath));
    }

    public ConfigurationBuilder updatePreference(JbillingAPI api, Integer preferenceTypeId, String preferenceValue){
        PreferenceTypeWS preferenceTypeWS=new PreferenceTypeWS();
        preferenceTypeWS.setId(preferenceTypeId);
        PreferenceWS preference = new PreferenceWS();
        preference.setValue(preferenceValue);
        preference.setPreferenceType(preferenceTypeWS);
        api.updatePreference(preference);
        return this;
    }
}
