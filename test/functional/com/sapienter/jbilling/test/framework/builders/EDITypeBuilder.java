package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.ediTransaction.EDIFileExceptionCodeWS;
import com.sapienter.jbilling.server.ediTransaction.EDIFileStatusWS;
import com.sapienter.jbilling.server.ediTransaction.EDITypeWS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by aman on 2/12/15.
 */
public class EDITypeBuilder extends AbstractBuilder {

    private String name;
    private String ediSuffix;
    private File formatFile;

    private List<EDIFileStatusWS> statuses = new LinkedList<EDIFileStatusWS>();

    public EDITypeBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        super(api, testEnvironment);
    }

    public EDITypeBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public EDITypeBuilder withEdiSuffix(String ediSuffix) {
        this.ediSuffix = ediSuffix;
        return this;
    }

    public EDITypeBuilder withFormatFile(File formatFile) {
        this.formatFile = formatFile;
        return this;
    }

    private EDIFileStatusWS createStatus(String statusName) {
        EDIFileStatusWS ediFileStatusWS = new EDIFileStatusWS();
        ediFileStatusWS.setName(statusName);
        return ediFileStatusWS;
    }

    public EDITypeBuilder withEDIStatusAndExceptionCodes(String statusName, String... exceptionCodes) {
        EDIFileStatusWS ediFileStatusWS = createStatus(statusName);
        for (String codeName : exceptionCodes) {
            EDIFileExceptionCodeWS exceptionCode = new EDIFileExceptionCodeWS();
            exceptionCode.setCode(codeName);
            ediFileStatusWS.getExceptionCodes().add(exceptionCode);
        }
        statuses.add(ediFileStatusWS);
        return this;
    }

    public EDITypeBuilder withEDIStatuses(String... ediStatuses) {
        for (String status : ediStatuses) {
            statuses.add(createStatus(status));
        }
        return this;
    }

    public EDITypeWS build() {
        EDITypeWS ediTypeWS = new EDITypeWS();
        ediTypeWS.setName(name);
        ediTypeWS.setEntityId(api.getCallerCompanyId());
        ediTypeWS.setEdiSuffix(ediSuffix);

        List entities = new LinkedList<Integer>();
        entities.add(api.getCallerCompanyId());
        ediTypeWS.getEntities().addAll(entities);

        ediTypeWS.setEdiStatuses(statuses);
        Integer typeId = api.createEDIType(ediTypeWS, formatFile);
        testEnvironment.add(ediTypeWS.getName(), typeId, ediTypeWS.getName(), api, TestEntityType.EDI_TYPE);
        ediTypeWS.setId(typeId);
        return ediTypeWS;
    }
}
