package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.ediTransaction.EDITypeWS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;

import java.util.*;

/**
 * Created by aman on 6/4/16.
 */
public class PluginBuilder extends AbstractBuilder {
    String code;
    Integer typeId;
    Integer order;
    Hashtable<String, String> parameters = new Hashtable<String, String>();

    public PluginBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        super(api, testEnvironment);
    }

    public PluginBuilder withCode(String code){
        this.code = code;
        return this;
    }

    public PluginBuilder withTypeId(Integer typeId){
        this.typeId =typeId;
        return this;
    }

    public PluginBuilder withOrder(Integer order){
        this.order=order;
        return this;
    }

    public PluginBuilder withParameter(String name, String value){
        parameters.put(name, value);
        return this;
    }

    public PluggableTaskWS build() {
        PluggableTaskWS plugin = new PluggableTaskWS();
        plugin.setTypeId(typeId);
        plugin.setProcessingOrder(order);

        plugin.setParameters(parameters);

        Integer id = api.createPlugin(plugin);
        testEnvironment.add(code, id, code, api, TestEntityType.PLUGGABLE_TASK);
        plugin.setId(id);
        return plugin;
    }
}
