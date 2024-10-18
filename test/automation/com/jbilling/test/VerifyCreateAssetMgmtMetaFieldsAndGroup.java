package com.jbilling.test;

import java.io.IOException;

import org.testng.annotations.Test;

import com.jbilling.framework.globals.GlobalEnumerations.TextComparators;
import com.jbilling.framework.globals.GlobalEnumsPage.AddMetaDataFields;
import com.jbilling.framework.globals.GlobalEnumsPage.AddMetaDataGroupFields;
import com.jbilling.framework.globals.GlobalEnumsPage.PageConfigurationItems;
import com.jbilling.framework.utilities.browserutils.BrowserApp;

public class VerifyCreateAssetMgmtMetaFieldsAndGroup extends BrowserApp {

    @Test(description = "TC 37: Verify that user is able to create "
            + "Asset Management Meta Fields and metafields group", groups = { "globalRegressionPack" })
    public void tc_0037_checkCreateAssetMgmtMetaDataAndGroupForUser () throws IOException {

        setTestRailsId("");

        navPage.navigateToConfigurationPage();
        confPage.selectConfiguration(PageConfigurationItems.MetaFields);
        confPage.clickMetaDataFieldValue("ASSET");
        confPage.clickAddNewButton();
        String strMetaFieldName = confPage.setNewMetaData(AddMetaDataFields.DATA_FIELD, "strMetaField", "anmf");
        propReader.updatePropertyInFile("strMetaField", strMetaFieldName, "testData");

        msgsPage.verifyDisplayedMessageText("MetaField", "created successfully.", TextComparators.contains);
        confPage.validateMetaSavedTestData(strMetaFieldName);
        confPage.clickAddNewButton();
        String intMetaFieldName = confPage.setNewMetaData(AddMetaDataFields.DATA_TYPE, "intMetaField", "asmf");
        propReader.updatePropertyInFile("intMetaField", intMetaFieldName, "testData");

        msgsPage.verifyDisplayedMessageText("MetaField", "created successfully.", TextComparators.contains);
        confPage.validateMetaSavedTestData(intMetaFieldName);
        confPage.clickAddNewButton();
        String boolMetaFieldName = confPage.setNewMetaData(AddMetaDataFields.DATA_DEFAULT_VALUE, "boolMetaField",
                "atmf");
        propReader.updatePropertyInFile("boolMetaField", boolMetaFieldName, "testData");

        msgsPage.verifyDisplayedMessageText("MetaField", "created successfully.", TextComparators.contains);
        confPage.validateMetaSavedTestData(boolMetaFieldName);
        confPage.selectConfiguration(PageConfigurationItems.MetaFieldGroups);
        confPage.clickMetaDataFieldValue("ASSET");
        confPage.clickAddNewButton();
        confPage.setNewMetaDataGroup(AddMetaDataGroupFields.GROUP_DATA_FIELD, "addMetaGroupName", "amdg",
                intMetaFieldName, boolMetaFieldName);
    }
}
