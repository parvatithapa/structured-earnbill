package com.sapienter.jbilling.server.spa;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Daily task to update the emergency address if the contact information or emergency address have been modified.
 * 
 * @author Leandro Bagur
 * @since 05/03/18.
 */
public class EmergencyAddressUpdateCurrentTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    public EmergencyAddressUpdateCurrentTask() {
        setUseTransaction(true);
    }
    
    @Override
    public String getTaskName() {
        return "emergency address update current: , entity id " + getEntityId() + ", taskId "
            + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Executing Emergency Address Update Current task");
        CustomerDAS customerDAS = new CustomerDAS();
        Date currentDate = Util.truncateDate(TimezoneHelper.companyCurrentDate(getEntityId()));

        AccountTypeDTO accountType = new AccountTypeDAS().findAccountTypeByName(getEntityId(), SpaConstants.ACCOUNT_TYPE_RESIDENTIAL);
        List<Integer> groupIds = accountType.getInformationTypes().stream()
            .filter(at -> SpaConstants.CONTACT_INFORMATION_AIT.equals(at.getName()) || SpaConstants.EMERGENCY_ADDRESS_AIT.equals(at.getName()))
            .map(AccountInformationTypeDTO::getId)
            .collect(Collectors.toList());
        
        List<CustomerDTO> customers = customerDAS.getCustomerByCAITEffectiveDateAndGroupIds(currentDate, groupIds);
        logger.info("Total customers to update: {}", customers.size());
        
        Integer userId;
        for (CustomerDTO customer : customers) {
            userId = customer.getBaseUser().getId();
            List<AssetWS> assets = new AssetBL().getAllAssetsByUserId(userId);
            Distributel911AddressUpdateEvent addressUpdateEvent = Distributel911AddressUpdateEvent.createEventForCustomerUpdateTask(getEntityId(), userId, assets);
            EventManager.process(addressUpdateEvent);    
        }
    }
}
