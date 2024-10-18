package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironment;

import java.util.Arrays;
import java.util.Optional;

/**
 * Created by aman on 16/3/16.
 */
public class CompanyBuilder extends AbstractMetaFieldBuilder<CompanyBuilder> {
    private JbillingAPI api;
    private TestEnvironment testEnvironment;

    public CompanyBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        this.api = api;
        this.testEnvironment = testEnvironment;
    }

    public void update() {
        CompanyWS company = api.getCompany();
        MetaFieldValueWS[] values = buildMetaField();
        if (values == null || values.length == 0) return;
        Arrays.stream(values).forEach(value->{
            Optional searchObject = Arrays.stream(company.getMetaFields()).filter(old-> old.getFieldName().equals(value.getFieldName())).findFirst();
                 if(searchObject.isPresent()){
                     MetaFieldValueWS modifyValue = (MetaFieldValueWS)searchObject.get();
                     modifyValue.setValue(value.getValue());
                 }
        });
        api.updateCompany(company);
    }
}
