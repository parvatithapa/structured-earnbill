package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.client.authentication.JBillingPasswordEncoder;
import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.company.event.NewAdminEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.*;
import com.sapienter.jbilling.server.user.contact.db.ContactDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.user.permisson.db.RoleDAS;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by vivek on 10/8/15.
 */
public class SystemAdminCopyTask extends AbstractCopyTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ConfigurationCopyTask.class));

    UserDAS userDAS;
    CompanyDAS companyDAS;
    ContactDAS contactDas;
    private Boolean isLogged = Boolean.TRUE;

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        return false;
    }

    public SystemAdminCopyTask() {
        init();
    }

    private void init() {
        userDAS = new UserDAS();
        companyDAS = new CompanyDAS();
        contactDas = new ContactDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("System admin creation has been started.");

        CompanyDTO targetEntity = companyDAS.find(targetEntityId);
        ContactDTO contact = contactDas.findEntityContact(entityId);

        Map<String, String> supportAdmin = Util.getMatchingProp(CommonConstants.ADMIN_USERS_REGEX);
        for (Map.Entry<String, String> entry : supportAdmin.entrySet()) {
            String[] credentials = entry.getValue().split(",");
            createUser(credentials, entityId, targetEntity,  Constants.TYPE_SYSTEM_ADMIN, contact.getCountryCode());
        }

        LOG.debug("All System Admin has been created.");
    }

    private String createUser(String [] credentials, Integer entityId, CompanyDTO targetEntity, Integer roleTypeId,
                              String countryCode) {

        String username = credentials[0];
        String email = getSysAdminEmail(credentials[1]);

        UserDTO userDTO = new UserDTO();
        userDTO.setUserName(username);

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

        RoleDTO roleDTO = new RoleDAS().findByRoleTypeIdAndCompanyId(roleTypeId, targetEntity.getId());
        Set<RoleDTO> roleDTOs = new HashSet<>();
        roleDTOs.add(roleDTO);
        userDTO.setRoles(roleDTOs);
        userDTO = userDAS.save(userDTO);

        createUserContact(email, targetEntity.getDescription(), userDTO.getId(), countryCode);

        Map<String, String> credentialMap = new HashMap<>();
        credentialMap.put(username, randPassword);

        //Ver porque llega duplicado
        EventManager.process(new NewAdminEvent(entityId, targetEntity.getId(), credentialMap, email, username));
        return randPassword;
    }

    private String getSysAdminEmail(String email) {
        Pattern pattern = Pattern.compile(CommonConstants.EMAIL_VALIDATION_REGEX);
        Matcher matcher = pattern.matcher(email);
        if(StringUtils.trimToNull(email) == null) {
            throw new SessionInternalError("Duplicate Description ",
                    new String[] { "system.admin.copy.email.null" });
        } else if(!matcher.matches()) {
            throw new SessionInternalError("Duplicate Description ",
                    new String[] { "system.admin.copy.email.validation" });
        }
        return email;
    }

    private void createUserContact(String email, String organizationName, Integer userId, String countryCode) {
        ContactWS contactWS = new ContactWS();
        contactWS.setEmail(email);
        contactWS.setInclude(true);
        contactWS.setCountryCode(countryCode);
        contactWS.setOrganizationName(organizationName);
        ContactDTOEx dtoEx = new ContactDTOEx(contactWS);
        IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean("webServicesSession");
        Integer callerID = isLogged ? webServicesSessionSpringBean.getCallerId() : null;
        new ContactBL().createForUser(dtoEx, userId, callerID);
    }

    public Boolean getLogged() {
        return isLogged;
    }

    public void setLogged(Boolean logged) {
        isLogged = logged;
    }
}
