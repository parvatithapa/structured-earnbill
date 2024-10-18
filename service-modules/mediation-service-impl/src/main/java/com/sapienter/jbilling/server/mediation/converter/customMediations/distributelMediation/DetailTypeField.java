/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */
package com.sapienter.jbilling.server.mediation.converter.customMediations.distributelMediation;

import java.lang.String;

/**
 * Created by igutierrez on 03/03/17.
 */
public enum DetailTypeField {
    NONE(0, ""), PDF(2,"pdf"), CSV(1,"csv"), PDF_CSV(3,"pdf_csv");

    Integer value;
    String extension;

    private DetailTypeField(Integer value, String extension) {
        this.value = value;
        this.extension = extension;
    }
    public Integer getValue(){
        return value;
    }
    public String getExtension() {
        return extension;
    }
}
