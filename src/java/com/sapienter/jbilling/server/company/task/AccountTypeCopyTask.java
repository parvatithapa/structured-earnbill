package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by vivek on 4/11/14.
 */
public class AccountTypeCopyTask extends AbstractCopyTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AccountTypeCopyTask.class));

    AccountTypeDAS accountTypeDAS = null;
    CompanyDAS companyDAS = null;

    private static final Class dependencies[] = new Class[]{
            PaymentMethodTypeCopyTask.class
    };

    public AccountTypeCopyTask() {
        init();
    }

    private void init() {
        accountTypeDAS = new AccountTypeDAS();
        companyDAS = new CompanyDAS();
    }
    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<AccountTypeDTO> copiedAccountTypeDTOList = accountTypeDAS.findAll(targetEntityId);
        return copiedAccountTypeDTOList != null && !copiedAccountTypeDTOList.isEmpty();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.info("Account Copy Task");

        copyAccountType(entityId, targetEntityId);
    }

    private void copyAccountType(Integer entityId, Integer targetEntityId) {
        CompanyDTO targetEntity = new CompanyDAS().find(targetEntityId);
        List<AccountTypeDTO> accountTypeDTOList = accountTypeDAS.findAll(entityId);

        for (AccountTypeDTO dto : accountTypeDTOList) {
            accountTypeDAS.reattach(dto);
            AccountTypeDTO copyAccountTypeDTO = new AccountTypeDTO();
            copyAccountTypeDTO.setCompany(targetEntity);
            copyAccountTypeDTO.setCreditLimit(dto.getCreditLimit());

            copyAccountTypeDTO.setInvoiceDesign(dto.getInvoiceDesign());
            copyAccountTypeDTO.setBillingCycle(dto.getBillingCycle());
            copyAccountTypeDTO.setCreditNotificationLimit1(dto.getCreditNotificationLimit1());
            copyAccountTypeDTO.setCreditNotificationLimit2(dto.getCreditNotificationLimit2());
            copyAccountTypeDTO.setInvoiceDeliveryMethod(dto.getInvoiceDeliveryMethod());
            copyAccountTypeDTO.setLanguage(dto.getLanguage());
            copyAccountTypeDTO.setCurrency(dto.getCurrency());
            copyAccountTypeDTO.setInformationTypes(dto.getInformationTypes());

            copyAccountTypeDTO.setDateCreated(TimezoneHelper.serverCurrentDate());

            Set<PaymentMethodTypeDTO> copyPaymentMethodTypeDTOs = new HashSet<PaymentMethodTypeDTO>();
            if (!dto.getPaymentMethodTypes().isEmpty()) {
                for (PaymentMethodTypeDTO paymentMethodTypeDTO : dto.getPaymentMethodTypes()) {
                    PaymentMethodTypeDTO copyPaymentMethodType = accountTypeDAS.findPaymentMethodByMethodName(paymentMethodTypeDTO.getMethodName(), targetEntityId);
                    copyPaymentMethodTypeDTOs.add(copyPaymentMethodType);
                }
                copyAccountTypeDTO.setPaymentMethodTypes(copyPaymentMethodTypeDTOs);
            }

            copyAccountTypeDTO = accountTypeDAS.save(copyAccountTypeDTO);
            copyAccountTypeDTO.setDescription(dto.getDescription(targetEntity.getLanguageId()), targetEntity.getLanguageId());
            copyAccountTypeDTO = accountTypeDAS.save(copyAccountTypeDTO);

            CopyCompanyUtils.oldNewAccountTypeMap.put(dto.getId(), copyAccountTypeDTO.getId());


        }
        accountTypeDAS.flush();
        accountTypeDAS.clear();
        LOG.debug("Account type copying has been finished.");
    }
}
