/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.util.api;

import com.sapienter.jbilling.server.util.RemoteContext;

import java.io.IOException;

public final class JbillingAPIFactory {
    
    private JbillingAPIFactory() {}; // a factory should not be instantiated
    
    static public JbillingAPI getAPI() 
            throws JbillingAPIException, IOException {
        return new SpringAPI();
    }
    
    /**
     * Additional API static method to get a Custom API object using a String name
     * @return
     * @throws JbillingAPIException
     * @throws IOException
     */
    static public JbillingAPI getAPI(String name) 
            throws JbillingAPIException, IOException {
        return new SpringAPI(name);
    }

    /**
     * Additional API static method to get a Custom API object using a RemoteContext.Name name
     * @return
     * @throws JbillingAPIException
     * @throws IOException
     */
    static public JbillingAPI getAPI(RemoteContext.Name name)
            throws JbillingAPIException, IOException {
        return new SpringAPI(name);
    }
}
