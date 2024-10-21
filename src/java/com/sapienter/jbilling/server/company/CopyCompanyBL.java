package com.sapienter.jbilling.server.company;

import com.sapienter.jbilling.client.authentication.JBillingPasswordEncoder;
import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.company.task.*;
import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.*;
import com.sapienter.jbilling.server.user.contact.db.ContactDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.user.permisson.db.RoleDAS;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

import java.util.*;

/**
 * Created by vivek on 30/10/14.
 */
public class CopyCompanyBL {
//    List<String> createEntities;

    private InvoiceDeliveryMethodDAS invoiceDeliveryMethodDAS;
    private AccountTypeDAS accountTypeDAS;
    private CompanyDAS companyDAS;
    private UserDAS userDAS;
    private IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean("webServicesSession");

    List<AbstractCopyTask> abstractCopyTasksList=new ArrayList<>();
    private static final Class copyTask[] = new Class[]{
            ConfigurationCopyTask.class,
            SystemAdminCopyTask.class,
            CategoryCopyTask.class,
            ProductCopyTask.class,
            PlanCopyTask.class,
            EDICopyTask.class,
            ReportCopyTask.class,
            ResourcesCopyTask.class
    };

    public Class[] getCopyTask() {
        return copyTask;
    }

    public CopyCompanyBL(){
        invoiceDeliveryMethodDAS = new InvoiceDeliveryMethodDAS();
        accountTypeDAS = new AccountTypeDAS();
        companyDAS = new CompanyDAS();
        userDAS = new UserDAS();
    }

    public UserWS copyCompany(String childCompanyTemplateName, Integer entityId, List<String> importEntities,
                              boolean isCompanyChild, boolean copyProducts, boolean copyPlans, String adminEmail) {

        synchronized (CopyCompanyUtils.oldNewMetaFieldMap) {
            int targetEntityId=0;
            try {
                Integer parentCompanyId = 0;
                //Need to set template company id in entity Id. Because new company will be copied from template company.
                parentCompanyId = entityId;
                if (isCompanyChild) {
                    //If creating child company. We need to create it from template company.
                    entityId = validateTemplate(childCompanyTemplateName);
                }

                CompanyDTO targetEntity = createCompany(entityId, isCompanyChild, parentCompanyId);
                copyLiquibaseChangeLogs(entityId, targetEntity.getId());
                UserWS copyUserWS = createUserWithRoles(entityId, targetEntity, adminEmail);
                targetEntityId = targetEntity.getId();
                createCompanyContact(entityId, targetEntity, copyUserWS.getUserId());

                for(Class task:getCopyTask()){
                    AbstractCopyTask abstractCopyTask=(AbstractCopyTask)task.newInstance();

                    if((!copyProducts && (abstractCopyTask instanceof CategoryCopyTask || abstractCopyTask instanceof ProductCopyTask)) ||
                    (!copyPlans && abstractCopyTask instanceof PlanCopyTask)){
                        continue;
                    }
                    abstractCopyTasksList.add(abstractCopyTask);
                    abstractCopyTask.create(entityId, targetEntityId);
                }

                copyUserWS.setEntityId(targetEntityId);
                return copyUserWS;
            } catch (Exception exception) {
                for(AbstractCopyTask copyTask:abstractCopyTasksList){
                    copyTask.cleanUp(targetEntityId);
                }
                throw new SessionInternalError(exception);
            } finally {
                resetCopyCompanyUtils();
            }
        }
    }

    public Integer validateTemplate(String childCompanyTemplateName){
        CompanyDTO templateChildCompany = companyDAS.findEntityByName(childCompanyTemplateName);
        if(templateChildCompany == null) {
            throw new SessionInternalError(
                    "Template Company does not exist ",
                    new String[]{"copy.company.child.template.not.exist," + childCompanyTemplateName.replaceAll(",", "&#44;")});
        }

        return  templateChildCompany.getId();
    }

    private void resetCopyCompanyUtils() {
        CopyCompanyUtils.oldNewUserMap = new HashMap<>();
        CopyCompanyUtils.oldNewCategoryMap = new HashMap<>();
        CopyCompanyUtils.oldNewItemMap = new HashMap<>();
        CopyCompanyUtils.oldNewAssetMap = new HashMap<>();
        CopyCompanyUtils.oldNewUsagePoolMap = new HashMap<>();
        CopyCompanyUtils.oldNewCurrencyExchangeMap = new HashMap<>();
        CopyCompanyUtils.oldNewOrderStatusMap = new HashMap<>();
        CopyCompanyUtils.oldNewOrderChangeStatusMap = new HashMap<>();
        CopyCompanyUtils.oldNewOrderPeriodMap = new HashMap<>();
        CopyCompanyUtils.oldNewPlanItemMap = new HashMap<>();
        CopyCompanyUtils.oldNewOrderMap = new HashMap<>();
        CopyCompanyUtils.oldNewAssetStatusMap = new HashMap<>();
        CopyCompanyUtils.oldNewMetaFieldMap = new HashMap<>();
        CopyCompanyUtils.oldNewEDITypeMap = new HashMap<>();
        CopyCompanyUtils.oldNewDataTableMap = new HashMap<>();
        CopyCompanyUtils.oldNewRouteRateCardMap = new HashMap<>();
    }

    public CompanyDTO createCompany(Integer entityId, boolean isCompanyChild, Integer parentCompanyId) {
        CompanyDTO oldCompany = companyDAS.find(entityId);
        CompanyDTO companyDTO = new CompanyDTO();
        companyDTO.setDescription(oldCompany.getDescription() + " copy " + System.currentTimeMillis());
        companyDTO.setCreateDatetime(TimezoneHelper.serverCurrentDate());
        companyDTO.setLanguage(oldCompany.getLanguage());
        companyDTO.setCurrency(oldCompany.getCurrency());
        companyDTO.setCurrencies(oldCompany.getCurrencies());
        companyDTO.setType(oldCompany.getType());
        companyDTO.setTimezone(oldCompany.getTimezone());
        companyDTO.setDeleted(0);
        if (isCompanyChild) {
            companyDTO.setParent(companyDAS.find(parentCompanyId));
        }
        companyDTO = companyDAS.save(companyDTO);
        companyDTO.getCurrency().getEntities_1().add(companyDTO.getId());
        companyDTO = companyDAS.save(companyDTO);
        // Set Invoice delivery Method Here. Its a constant for all account type.
        setInvoiceDeliveryMethod(companyDTO);
        return companyDTO;
    }

    private void copyLiquibaseChangeLogs(Integer oldCompany, Integer newCompany){
        companyDAS.copyLiquibaseChangeLogs(oldCompany, newCompany, "enrollment-edi-communication.xml");
        companyDAS.copyLiquibaseChangeLogs(oldCompany, newCompany, "parent-company-account-type.xml");
    }

    private void setInvoiceDeliveryMethod(CompanyDTO targetEntity) {
        List<InvoiceDeliveryMethodDTO> invoiceDeliveryMethodDTOs =  invoiceDeliveryMethodDAS.findAll();
        for(InvoiceDeliveryMethodDTO invoiceDeliveryMethodDTO : invoiceDeliveryMethodDTOs) {
            invoiceDeliveryMethodDTO.getEntities().add(targetEntity);
            invoiceDeliveryMethodDAS.save(invoiceDeliveryMethodDTO);
        }
    }

    public UserWS createUserWithRoles(Integer entityId, CompanyDTO targetEntity, String adminEmail) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserName("admin");

        String randPassword = UserBL.generatePCICompliantPassword();
        JBillingPasswordEncoder passwordEncoder = new JBillingPasswordEncoder();
        userDTO.setPassword(passwordEncoder.encodePassword(randPassword, null));

        userDTO.setDeleted(0);
        userDTO.setUserStatus(new UserStatusDAS().find(UserDTOEx.STATUS_ACTIVE));
        userDTO.setSubscriberStatus(new SubscriberStatusDAS().find(UserDTOEx.SUBSCRIBER_ACTIVE));
        userDTO.setLanguage(targetEntity.getLanguage());
        userDTO.setCurrency(targetEntity.getCurrency());
        userDTO.setCompany(targetEntity);
        userDTO.setCreateDatetime(TimezoneHelper.serverCurrentDate());
        userDTO.setEncryptionScheme(Integer.parseInt(Util.getSysProp(com.sapienter.jbilling.server.util.Constants.PASSWORD_ENCRYPTION_SCHEME)));

        CompanyDTO entity = new CompanyDAS().find(entityId);
        new RoleBL().createDefaultRoles(targetEntity.getLanguageId(), entity, targetEntity);

        RoleDTO roleDTO = new RoleDAS().findByRoleTypeIdAndCompanyId(Constants.TYPE_ROOT, targetEntity.getId());
        Set<RoleDTO> roleDTOs = new HashSet<RoleDTO>();
        roleDTOs.add(roleDTO);
        userDTO.setRoles(roleDTOs);

        userDTO = new UserDAS().save(userDTO);
        UserWS userWS = UserBL.getWS(new UserDTOEx(userDTO));
        userWS.setPassword(randPassword);

        ContactDTO contactEntity = new ContactDAS().findEntityContact(entityId);
        ContactDTOEx contact = new ContactDTOEx();
        contact.setInclude(1);
        contact.setEmail(adminEmail);
        if(contactEntity != null) {
            contact.setCountryCode(contactEntity.getCountryCode());
        }

        new ContactBL().createForUser(contact, userWS.getId(), webServicesSessionSpringBean.getCallerId());
        return userWS;
    }

    public void createCompanyContact(Integer entityId, CompanyDTO targetEntity, Integer user) {
        ContactDTO contact = new EntityBL(entityId).getContact();
        new ContactBL().create(new ContactDTOEx(contact), com.sapienter.jbilling.server.util.Constants.TABLE_ENTITY, targetEntity.getId(),
                user);
    }

}
