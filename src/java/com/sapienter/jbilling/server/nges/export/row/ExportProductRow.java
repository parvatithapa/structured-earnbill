/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.nges.export.row;

/**
 * Created by hitesh on 28/9/16.
 */
public class ExportProductRow extends ExportRow {

    //(M) LDC Name.
    private String companyName;
    //(M) Product Name.
    private String productId;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public static String getHeader() {
        return "LDC,Product_Id";
    }

    public static String getErrorFileHeader() {
        return "plan_id,error_message";
    }

    @Override
    public String getRow() {
        return super.row = companyName + "," + productId;
    }
}
