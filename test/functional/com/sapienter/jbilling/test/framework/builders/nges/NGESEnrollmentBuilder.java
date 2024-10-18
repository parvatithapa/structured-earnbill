package com.sapienter.jbilling.test.framework.builders.nges;

import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentStatus;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.builders.AbstractMetaFieldBuilder;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Created by aman on 4/4/16.
 */
public class NGESEnrollmentBuilder extends AbstractMetaFieldBuilder<NGESEnrollmentBuilder> {
    private JbillingAPI api;
    private TestEnvironment testEnvironment;

    private String code;
    private NGESAccountTypeBuild.AccountType accountType;
    private CustomerEnrollmentStatus status;

    public NGESEnrollmentBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        this.api = api;
        this.testEnvironment = testEnvironment;
    }

    public NGESEnrollmentBuilder withCode(String code) {
        this.code = code;
        return this;
    }

    public NGESEnrollmentBuilder withAccountType(NGESAccountTypeBuild.AccountType accountType) {
        this.accountType = accountType;
        return this;
    }

    public NGESEnrollmentBuilder withStatus(CustomerEnrollmentStatus status) {
        this.status = status;
        return this;
    }

    public NGESEnrollmentBuilder withAITGroup(NGESAccountTypeBuild.AIT aitGroup, Map<String, Object> values) {
        Integer accountTypeId = testEnvironment.idForCode(accountType.toString());
        if (accountType == null) {
            throw new IllegalStateException("Account type should exist");
        }
        AccountInformationTypeWS[] aits = api.getInformationTypesForAccountType(accountTypeId);
        Optional searchObject = Arrays.stream(aits).filter(ait -> ait.getName().equals(aitGroup.getName())).findFirst();
        if (!searchObject.isPresent()) {
            throw new IllegalStateException("No valid AIT found");
        }
        final AccountInformationTypeWS accountInformation = (AccountInformationTypeWS) searchObject.get();
        values.entrySet().stream().forEach(e-> withMetaField(e.getKey(), e.getValue(), accountInformation.getId()));
        return this;
    }


    public Integer build() {
        CustomerEnrollmentWS customerEnrollmentWS = new CustomerEnrollmentWS();
        Integer accountTypeId = testEnvironment.idForCode(accountType.toString());

        customerEnrollmentWS.setAccountTypeId(accountTypeId);

        customerEnrollmentWS.setId(0);

        customerEnrollmentWS.setDeleted(0);

        customerEnrollmentWS.setEntityId(api.getCallerCompanyId());
        customerEnrollmentWS.setCreateDatetime(new Date());
        customerEnrollmentWS.setStatus(status);

        customerEnrollmentWS.setMetaFields(buildMetaField());

        Integer id = api.createUpdateEnrollment(customerEnrollmentWS);
        testEnvironment.add(code, id, code, api, TestEntityType.ENROLLMENT);
        return id;
    }
}
