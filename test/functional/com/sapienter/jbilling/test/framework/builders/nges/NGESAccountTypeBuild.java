package com.sapienter.jbilling.test.framework.builders.nges;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.test.framework.builders.AccountTypeBuilder;
import static com.sapienter.jbilling.server.fileProcessing.FileConstants.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by aman on 14/3/16.
 */
public class NGESAccountTypeBuild {
    private AccountTypeBuilder accountTypeBuilder;

    public enum AccountType {
        Residential,
        Commercial
    }

    public enum AIT {
        //ToDo: change variable name
        Customer_Information("Service Information", new HashMap<String, DataType>() {{
            put(DIVISION, DataType.ENUMERATION);
            put(NAME, DataType.STRING);
            put("ADDRESS1", DataType.STRING);
            put("ADDRESS2", DataType.STRING);
            put("CITY", DataType.STRING);
            put("STATE", DataType.ENUMERATION);
            put("ZIP_CODE", DataType.STRING);
            put("TELEPHONE", DataType.STRING);
            put("Email", DataType.STRING);
        }}),

        Business_Information("Service Information", new HashMap<String, DataType>() {{
            put("DIVISION", DataType.ENUMERATION);
            put("NAME", DataType.STRING);
            put("ADDRESS1", DataType.STRING);
            put("ADDRESS2", DataType.STRING);
            put("CITY", DataType.STRING);
            put("STATE", DataType.ENUMERATION);
            put("ZIP_CODE", DataType.STRING);
            put("TELEPHONE", DataType.STRING);
            put("Email", DataType.STRING);
        }}),
        Account_Information("Account Information", new HashMap<String, DataType>() {{
            put(COMMODITY, DataType.ENUMERATION);
            put(DURATION, DataType.ENUMERATION);
            put(PLAN, DataType.ENUMERATION);
            put(CUSTOMER_ACCOUNT_KEY, DataType.STRING);
            put(ACTUAL_START_DATE, DataType.DATE);
            put(CUST_LIFE_SUPPORT, DataType.BOOLEAN);
            put("METER_TYPE", DataType.ENUMERATION);
            put("Notification Method", DataType.ENUMERATION);
            put(DEFAULT_PLAN, DataType.STRING);
        }}),
        Contact_Information("Billing Information", new HashMap<String, DataType>() {{
            put("NAME", DataType.STRING);
            put("ADDRESS1", DataType.STRING);
            put("ADDRESS2", DataType.STRING);
            put("CITY", DataType.STRING);
            put("STATE", DataType.ENUMERATION);
            put("ZIP_CODE", DataType.STRING);
            put("TELEPHONE", DataType.STRING);
        }});

        private String name;
        private HashMap metaFields;

        AIT(String name, HashMap metaFields) {
            this.name = name;
            this.metaFields = metaFields;
        }

        public String getName() {
            return name;
        }

        public HashMap getMetaFields(){
            return metaFields;
        }
    }

    public NGESAccountTypeBuild(AccountTypeBuilder accountTypeBuilder) {
        this.accountTypeBuilder = accountTypeBuilder;
    }

    public void buildResidentialAccountType() {
        accountTypeBuilder.withName(AccountType.Residential.toString()).useExactDescription(true).addAccountInformationType(AIT.Customer_Information.getName(), AIT.Customer_Information.getMetaFields());
    }

    public void buildCommercialAccountType() {
        accountTypeBuilder.withName(AccountType.Commercial.toString()).useExactDescription(true)
                .addAccountInformationType(AIT.Business_Information.getName(), AIT.Business_Information.getMetaFields())
                .addAccountInformationType(AIT.Contact_Information.getName(), AIT.Contact_Information.getMetaFields());
    }

    public void addAccountInformationAIT() {
        accountTypeBuilder.addAccountInformationType(AIT.Account_Information.getName(), AIT.Account_Information.getMetaFields());
    }
}
