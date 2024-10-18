/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.customer.task;

import java.text.SimpleDateFormat;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sapienter.jbilling.client.mcf.MCFServiceBL;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.customer.event.UpdateCustomerEvent;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateDAS;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderChangePlanItemDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.provisioning.task.IExternalProvisioning;
import com.sapienter.jbilling.server.provisioning.task.MCFExternalProvisioningTask;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.SpaImportBL;
import com.sapienter.jbilling.server.spa.SpaImportHelper;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserHelperDisplayerDistributel;
import com.sapienter.jbilling.server.user.balance.CustomerProperties;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.Constants;

/**
 * Created by igutierrez on 3/28/17.
 */
public class UpdateDistributelCustomerTask extends PluggableTask implements IInternalEventsTask {
    private static final FormatLogger log = new FormatLogger(Logger.getLogger(UpdateDistributelCustomerTask.class));

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[]{
        UpdateCustomerEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        if (event instanceof UpdateCustomerEvent) {
            processUpdateDistributelCustomer((UpdateCustomerEvent) event);
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private void processUpdateDistributelCustomer(UpdateCustomerEvent event) {
        boolean changeAccount = false;
        CustomerDTO customer = new CustomerDAS().findNow(event.getCustomerId());
        CustomerProperties oldCustomer = event.getOldCustomer();
        Integer entityId = customer.getBaseUser().getCompany().getId();
        String newLanguage = customer.getBaseUser().getLanguage().getCode();
        String oldLanguage = oldCustomer.getLanguageCode();
        if (!newLanguage.equals(oldLanguage)) {
            Integer templateId = new SpaImportBL().getInvoiceTemplateId(entityId, SpaImportHelper.getLanguageByCode(newLanguage));
            if (templateId != null) {
                InvoiceTemplateDTO invoiceTemplate = new InvoiceTemplateDAS().find(templateId);
                customer.setInvoiceTemplate(invoiceTemplate);
            }
            changeAccount = true;
        }

        AccountInformationTypeDTO contactInformationAIT = customer.getAccountType().getInformationTypes().stream().filter(at -> at.getName().equals(SpaConstants.CONTACT_INFORMATION_AIT)).findFirst().get();
        Integer contactInformationAITid = contactInformationAIT.getId();
        CustomerAccountInfoTypeMetaField customerAitMF = customer.getCurrentCustomerAccountInfoTypeMetaField(SpaConstants.CUSTOMER_NAME, contactInformationAITid) ;
        if (customerAitMF != null) {
            String customerName = customerAitMF.getMetaFieldValue().getValue().toString();
            String userName = customer.getBaseUser().getUserName();
            userName = userName.indexOf("_") > 0 ? userName.substring(0, userName.indexOf("_")) : userName;
            if (!StringUtils.equals(customerName, userName)) {
                String name = customerName + "_" + System.currentTimeMillis();
                customer.getBaseUser().setUserName(name);
                changeAccount = true;
            }
        }

        updateMFCAccount(changeAccount, customer);
    }

    private void updateMFCAccount(boolean changeAccount, CustomerDTO customer) {
        if (!changeAccount) {
            return;
        }
        if (!isMCFCustomer(customer)) {
            return;
        }
        MCFExternalProvisioningTask mcfExternalProvisioningTask = getMCFExternalProvisioningTask(customer);
        MCFServiceBL mcfServiceBL = new MCFServiceBL(mcfExternalProvisioningTask.getParameters().get(mcfExternalProvisioningTask.PARAMETER_MCF_REQUEST_URL.getName()));
        MetaFieldValue businessUnitMF = customer.getBaseUser().getCompany().getMetaField(SpaConstants.BUSINESS_UNIT);
        String businessUnit = businessUnitMF != null ? (String) businessUnitMF.getValue() : "No Business Unit";
        String effectiveDate = new SimpleDateFormat("yyyyMMdd").format(this.companyCurrentDate());
        String accountNumber = String.valueOf(customer.getBaseUser().getId());
        String accountName = UserHelperDisplayerDistributel.getInstance().getDisplayName(customer.getBaseUser());
        String language = SpaImportHelper.getLanguageByCode(customer.getBaseUser().getLanguage().getCode());
        String result = mcfServiceBL.sendMODACCCommand(businessUnit, effectiveDate, accountNumber, accountName, language);
        log.debug("Result sending MODACC command: " + result);
    }

    private boolean isMCFCustomer(CustomerDTO customer) {
        for (OrderDTO order : new OrderDAS().findAllUserByUserId(customer.getBaseUser().getUserId())) {
            for (OrderChangeDTO orderChange : new OrderChangeDAS().findByOrder(order.getId())) {
                for (OrderChangePlanItemDTO orderChangePlanItem : orderChange.getOrderChangePlanItems()) {
                    for (ItemTypeDTO category : orderChangePlanItem.getItem().getItemTypes()) {
                        if (SpaConstants.MC_RATED_CATEGORY.equals(category.getDescription())) {
                            return true;
                        }
                    }
                }
                if (orderChange.getItem().getItemTypes().stream().anyMatch(category -> SpaConstants.MC_RATED_CATEGORY.equals(category.getDescription()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private MCFExternalProvisioningTask getMCFExternalProvisioningTask(CustomerDTO customer) {
        MCFExternalProvisioningTask mcfExternalProvisioningTask = null;
        PluggableTaskManager<IExternalProvisioning> taskManager =
                null;
        try {
            taskManager = new PluggableTaskManager<IExternalProvisioning>(customer.getBaseUser().getCompany().getId(),
                    Constants.PLUGGABLE_TASK_EXTERNAL_PROVISIONING);
            for (IExternalProvisioning task = taskManager.getNextClass(); task != null; task = taskManager.getNextClass()) {
                if (task instanceof MCFExternalProvisioningTask) {
                    mcfExternalProvisioningTask = (MCFExternalProvisioningTask) task;
                    break;
                }
            }
        } catch (PluggableTaskException e) {
            log.error(e);
        }
        return mcfExternalProvisioningTask;
    }


}
