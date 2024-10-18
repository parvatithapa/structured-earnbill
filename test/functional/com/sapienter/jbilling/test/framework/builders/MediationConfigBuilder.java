package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEntityType;

/**
 * Created by marcolin on 06/11/15.
 */
public class MediationConfigBuilder extends AbstractBuilder{

    private String name;
    private String launcher;
    private boolean global = false;
    private String localInputDirectory;

    private MediationConfigBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        super(api, testEnvironment);
    }

    public static MediationConfigBuilder getBuilder(JbillingAPI api, TestEnvironment testEnvironment){
        return new MediationConfigBuilder(api, testEnvironment);
    }

    public MediationConfigBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public MediationConfigBuilder withLauncher(String launcher) {
        this.launcher = launcher;
        return this;
    }

    public MediationConfigBuilder global(boolean global){
        this.global = global;
        return this;
    }

    public MediationConfigBuilder withLocalInputDirectory(String localInputDirectory) {
        this.localInputDirectory = localInputDirectory;
        return this;
    }

    public Integer build() {
        MediationConfigurationWS config = new MediationConfigurationWS();
        config.setName(name +  System.currentTimeMillis());
        config.setMediationJobLauncher(launcher);
        config.setGlobal(Boolean.valueOf(global));
        config.setOrderValue("1");
        config.setLocalInputDirectory(localInputDirectory);
        Integer mediationConfigurationId = api.createMediationConfiguration(config);
        testEnvironment.add(name, mediationConfigurationId, config.getName(), api, TestEntityType.MEDIATION_CONFIGURATION);
        return mediationConfigurationId;
    }
}
