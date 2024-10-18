/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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
package jbilling

import javax.persistence.FetchType
import javax.persistence.OneToOne

/**
 * Objects of this class represents an horizontal menu item at the top of the screen (customers,products...)
 *
 * Either accessUrl or requiredRole must be supplied. If both are supplied the requiredRole takes precedence
 */
class Tab {

    //Message to display on the tab
    String messageCode;
    //User will be redirected to this controller when he clicks the link
    String controllerName;
    //User must have access to the URL
    String accessUrl;
    //User must have this role
    String requiredRole;
    //default order the tab is displayed in
    Integer defaultOrder;

    Tab parentTab

    @Override
    public String toString() {
        return "Tab{" +
                "id=" + id +
                ", messageCode='" + messageCode + '\'' +
                ", controllerName='" + controllerName + '\'' +
                ", accessUrl='" + accessUrl + '\'' +
                ", requiredRole='" + requiredRole + '\'' +
                '}';
    }
    static mapping = {
        parentTab lazy: false
    }
}
