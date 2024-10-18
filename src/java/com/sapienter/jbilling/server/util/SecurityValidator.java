package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.customer.CustomerBL;
import com.sapienter.jbilling.server.security.HierarchicalEntity;
import com.sapienter.jbilling.server.security.MappedSecuredWS;
import com.sapienter.jbilling.server.security.Validator;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.PartnerBL;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;

import grails.plugin.springsecurity.SpringSecurityService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Security validator utility used to validate if the user who do the call is permitted to access to certain entity.
 *
 * @since 05-27-2015
 */
public class SecurityValidator {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(SecurityValidator.class));

    private SpringSecurityService springSecurityService;
    private CompanyDAS companyDAS = new CompanyDAS();
    private UserDAS userDAS = new UserDAS();

    public SecurityValidator (SpringSecurityService springSecurityService) {
        this.springSecurityService = springSecurityService;
    }

    /**
     * Validates that the given owningEntityId matches the entity of the user account making the web-service call.
     *
     * @param inputObject entity that's being accessed
     * @param validatorType enum used to change method logic depending its validation type
     * @throws SecurityException thrown if user is accessing data that does not belong to them
     */
    public void validateCompany(WSSecured inputObject, Integer owningEntityId, Validator.Type validatorType) throws SecurityException {

        if ( null != inputObject && (null != inputObject.getOwningEntityId() || null != owningEntityId)) {
            CompanyUserDetails userPrincipal = getPrincipal();

            if (userPrincipal.getCompanyId().equals(inputObject.getOwningEntityId())
                    || userPrincipal.getCompanyId().equals(owningEntityId)) {
                return;
            }

            boolean allow= false;
            if( Validator.Type.EDIT.equals(validatorType)) {
                if (userPrincipal.getCompanyId().equals(inputObject.getOwningEntityId())
                        || userPrincipal.getCompanyId().equals(owningEntityId)) {
                    allow= true;
                }
            } else if ( inputObject instanceof HierarchicalEntity ) {

                HierarchicalEntity hierarchyEntity= ((HierarchicalEntity) inputObject);

                if ( null != hierarchyEntity.getAccessEntities()) {
                    if ( hierarchyEntity.getAccessEntities().contains(userPrincipal.getCompanyId()) ) {
                        allow= true;
                    }
                } else if (Boolean.TRUE.equals(hierarchyEntity.ifGlobal()) ) {
                    List<Integer> authorizedEntities = companyDAS.findAllHierarchyEntitiesIds(userPrincipal.getCompanyId());
                    if ( authorizedEntities.contains( inputObject.getOwningEntityId() )
                            || authorizedEntities.contains(owningEntityId)) {
                        allow= true;
                    }
                }
            }else if ( inputObject instanceof MappedSecuredWS ) { //This code allows parent company to view customer details of child company
            	 if( Validator.Type.VIEW.equals(validatorType)) {
            		 allow = isValidParentChildRelation(inputObject, owningEntityId, userPrincipal);
            	 }
            }

            if (!allow) {
                String message = String.format("Unauthorized access to entity %s by caller '%s' (id %s)",
                        (inputObject.getOwningEntityId() != null ?
                                inputObject.getOwningEntityId() :
                                owningEntityId), userPrincipal.getUsername(), userPrincipal.getCompanyId());
                LOG.warn(message);
                throw new SecurityException(message);
            }
        }
    }

	/** This method validates company hierarchy by UserWS
	 * @param inputObject
	 * @param owningEntityId
	 * @param userPrincipal
	 * @return
	 */
	private boolean isValidParentChildRelation(WSSecured inputObject,
			Integer owningEntityId, CompanyUserDetails userPrincipal
			) {
		
			UserBL userBL = new UserBL(inputObject.getOwningUserId());
			UserWS userWS = userBL.getUserWS();
			if(userWS !=null){
				HierarchicalEntity hierarchyEntity= ((HierarchicalEntity) userWS);

		        if ( null != hierarchyEntity.getAccessEntities()) {
		            if ( hierarchyEntity.getAccessEntities().contains(userPrincipal.getCompanyId()) ) {
		                return true;
		            }
		        } else if (Boolean.TRUE.equals(hierarchyEntity.ifGlobal()) ) {
		            List<Integer> authorizedEntities = companyDAS.findAllHierarchyEntitiesIds(userPrincipal.getCompanyId());
		            if ( authorizedEntities.contains( userWS.getOwningEntityId() )
		                    || authorizedEntities.contains(owningEntityId)) {
		                return true;
		            }
		        }
			}
		return false;
	}

    //For use from controllers, for checking pre-Gorm access
    public boolean checkValidateCompany(Integer owningEntityId)  {
        if ( null == owningEntityId ) {
            return true;
        }
        CompanyUserDetails userPrincipal = getPrincipal();

        return ( owningEntityId.equals(userPrincipal.getCompanyId()));
    }

    //For use from controllers, for checking pre-Gorm access
    public void validateCompany(Integer owningEntityId, Validator.Type validatorType) throws SecurityException {
        if ( !checkValidateCompany(owningEntityId)) {
            CompanyUserDetails userPrincipal = getPrincipal();
            String message = String.format("Unauthorized access to entity %s by caller '%s' (id %s)",
                    owningEntityId, userPrincipal.getUsername(), userPrincipal.getCompanyId());
            LOG.warn(message);
            throw new SecurityException(message);
        }
    }

    //For use from controllers, for checking pre-Gorm access
    public void validateUser(Integer userId) throws SecurityException {
        if ( null == userId ) {
            return;
        }
        CompanyUserDetails userPrincipal = getPrincipal();

        if ( ! userId.equals(userPrincipal.getUserId())) {
            throw new SecurityException(String.format("Unauthorized access to user %s by caller '%s' (id %s)",
                    userId, userPrincipal.getUsername(), userPrincipal.getUserId()));
        }
    }

    //For use from controllers, for checking pre-Gorm access
    public void validateCompany(Integer owningEntityId, List<Integer> accessEntities, Boolean global, Validator.Type validatorType) throws SecurityException {

        if ( null != owningEntityId ) {
            CompanyUserDetails userPrincipal = getPrincipal();

            if ( owningEntityId.equals( userPrincipal.getCompanyId() )) {
                return;
            }

            boolean allow= false;
            if( Validator.Type.EDIT.equals(validatorType)) {
                if ( userPrincipal.getCompanyId().equals( owningEntityId ) ) {
                    allow= true;
                }
            } else if ( !CollectionUtils.isEmpty(accessEntities) ) {
                if ( accessEntities.contains(userPrincipal.getCompanyId()) ) {
                        allow= true;
                }
            } else {
                List<Integer> authorizedEntities = companyDAS.findAllHierarchyEntitiesIds(userPrincipal.getCompanyId());
                if ( Boolean.TRUE.equals(global) && authorizedEntities.contains( owningEntityId )) {
                    allow= true;
                }
            }

            if (!allow) {
                String message = String.format("Unauthorized access to entity %s by caller '%s' (id %s)",
                        owningEntityId, userPrincipal.getUsername(), userPrincipal.getCompanyId());
                LOG.warn(message);
                throw new SecurityException(message);
            }
        }
    }

    /**
     * Validates that the entity of the user account making the call matches with any of the given owning
     * entity hierarchy.
     *
     * @param owningEntityHierarchy entities id which own the data being accessed
     * @throws SecurityException thrown if user is accessing data that does not belong to them
     */
    public void validateCompanyHierarchy(List<Integer> owningEntityHierarchy) throws SecurityException {
        CompanyUserDetails userPrincipal = getPrincipal();
        if(!CollectionUtils.containsAny(companyDAS.getCurrentAndDescendants(userPrincipal.getCompanyId()), owningEntityHierarchy)) {
            String message = "Unauthorized access by caller '" + userPrincipal.getUsername() + "' (id " + userPrincipal.getCompanyId() + ")";
            LOG.warn(message);
            throw new SecurityException(message);
        }
    }

    /**
     * Validates that the entity of the user account making the call matches with any of the given owning
     * entity valid hierarchy 
     *
     * @param owningEntityHierarchy entities id which own the data being accessed
     * @throws SecurityException thrown if user is accessing data that does not belong to them
     */
    public void validateCompanyHierarchy(List<Integer> owningEntityHierarchy, Integer entityId, Boolean isGlobal) throws SecurityException {
        validateCompanyHierarchy(owningEntityHierarchy, entityId, isGlobal, false);
    }

    public void validateCompanyHierarchy(List<Integer> owningEntityHierarchy, Integer entityId, Boolean isGlobal, Boolean parentAllow) throws SecurityException {
        if(owningEntityHierarchy!=null || entityId !=null) {
            boolean allowed = false;
            CompanyUserDetails userPrincipal = getPrincipal();
            if (entityId != null) {
                if (isGlobal != null && isGlobal) {
                    if (companyDAS.getCurrentAndDescendants(entityId).contains(userPrincipal.getCompanyId())) {
                        allowed = true;
                    }
                } else {
                    if (userPrincipal.getCompanyId().equals(entityId)) {
                        allowed = true;
                    }else if(parentAllow){
                        CompanyDTO entity = companyDAS.findNow(entityId);
                        if(entity != null && entity.getParent() != null){
                            allowed = entity.getParent().getId() == userPrincipal.getCompanyId() &&
                                            !entity.isInvoiceAsReseller();
                        }
                    }
                }
            }
            if (owningEntityHierarchy != null && !owningEntityHierarchy.isEmpty()) {
                if (isGlobal != null && isGlobal) {
                    ArrayList<Integer> validEntities = new ArrayList<>();
                    for (Integer entity : owningEntityHierarchy) {
                        validEntities.addAll(companyDAS.getCurrentAndDescendants(entity));
                    }
                    if (validEntities.contains(userPrincipal.getCompanyId())) {
                        allowed = true;
                    }
                } else {
                    if (owningEntityHierarchy.contains(userPrincipal.getCompanyId())) {
                        allowed = true;
                    }
                }
            }

            if (!allowed) {
                String msg = "Unauthorized access by caller '" + userPrincipal.getUsername() + "' (id " + userPrincipal.getCompanyId() + ")";
                LOG.warn(msg);
                throw new SecurityException(msg);
            }
        }
    }

    /**
     * Validates that the given owningUserId is a descendants or the same user which is making the call depending on its
     * validation type.
     *
     * @param owningUserId user id owning the data being accessed
     * @param validatorType enum used to change method logic depending its validation type
     * @param allowAgent boolean used for give to the agent permissions to edit only Orders & Customers
     * @throws SecurityException thrown if user is accessing data that does not belonging to them
     */
    private void validateUser(WSSecured inputObject, Integer owningUserId, Integer owningUserEntityId,
                              Validator.Type validatorType, boolean allowAgent) {
        CompanyUserDetails userPrincipal = getPrincipal();

        //If callerUserID is admin do nothing
        Integer callerUserId = userPrincipal.getUserId();
        UserDTO user = userDAS.find(callerUserId);
        Integer companyId = userPrincipal.getCompanyId();
        List<Integer> adminUsers = userDAS.findAdminUserIds(companyId);
        if (!CollectionUtils.isEmpty(adminUsers) && adminUsers.contains(callerUserId)) {
            if (companyId.equals( owningUserEntityId )) {
                return;
            }
        }

        //If callerUserID is clerk do nothing
        List<Integer> clerkUsers = userDAS.findClerkUserIds(companyId);
        if (!CollectionUtils.isEmpty(clerkUsers) && clerkUsers.contains(callerUserId)) {
            if (companyId.equals(owningUserEntityId)) {
                return;
            }
        }

        //If the user does not have a Customer then do nothing
        CustomerDTO callerCustomer = user.getCustomer();
        if ( null != callerCustomer ) {
            if (callerUserId.equals(owningUserId)) {
                return;
            }
        }

        //If callerUserId is agent do nothing
        if(user.getRoles().stream()
                          .map(RoleDTO::getRoleTypeId)
                          .anyMatch(CommonConstants.TYPE_PARTNER::equals)){
            if (callerUserId.equals(owningUserId)) {
                return;
            } else if (Validator.Type.EDIT.equals(validatorType) && allowAgent &&
                       user.getEntity().getId() == owningUserEntityId) {
                return;
            }
        }

        if( Validator.Type.VIEW.equals(validatorType)) {
            if (inputObject instanceof HierarchicalEntity) {
                HierarchicalEntity hierarchyEntity = ((HierarchicalEntity) inputObject);
                if (null != hierarchyEntity.getAccessEntities()) {
                    if (hierarchyEntity.getAccessEntities().contains(userPrincipal.getCompanyId())) {
                        return;
                    }
                } else if (Boolean.TRUE.equals(hierarchyEntity.ifGlobal())) {
                    List<Integer> authorizedEntities = companyDAS.findAllHierarchyEntitiesIds(userPrincipal.getCompanyId());
                    if (authorizedEntities.contains(inputObject.getOwningEntityId())
                            || authorizedEntities.contains(owningUserEntityId)) {
                        return;
                    }
                }
            } else if (callerCustomer == null && (inputObject instanceof MappedSecuredWS)) { //This code allows parent company to view customer details of child company
            	if(isValidParentChildRelation(inputObject, owningUserEntityId, userPrincipal)){
            		return;
            	}
            } else {
                //Obtain descendants users
                List<Integer> authorizedUsers = new CustomerBL().getDescendants(callerCustomer);
                if (authorizedUsers.contains(owningUserId)) {
                    return;
                }
            }
        }

        String message = String.format("Unauthorized access to entity %s for customer %s data by caller '%s' (id %s)",
                owningUserEntityId, owningUserId, userPrincipal.getUsername(), userPrincipal.getCompanyId());
        LOG.warn(message);
        throw new SecurityException(message);

    }

    public void throwException() {
        CompanyUserDetails userPrincipal = getPrincipal();
        throw new SecurityException("Unauthorized access by caller '" + userPrincipal.getUsername() + "' (id " + userPrincipal.getCompanyId() + ")");
    }

    /**
     * Method in charge of calling validate user and validate company methods.
     *
     * @param accessedObject object the data being accessed
     * @param validatorType enum used to change method logic depending its validation type
     */
    public void validateUserAndCompany(WSSecured accessedObject, Validator.Type validatorType) {
        validateUserAndCompany(accessedObject, validatorType, false);
    }

    /**
     * Method in charge of calling validate user and validate company methods.
     *
     * @param accessedObject object the data being accessed
     * @param validatorType enum used to change method logic depending its validation type
     * @param allowAgent boolean used for give to the agent permissions to edit only Orders & Customers
     */
    public void validateUserAndCompany(WSSecured accessedObject, Validator.Type validatorType, boolean allowAgent) {
        if (userDAS.isIdPersisted(accessedObject.getOwningUserId())) {
            UserDTO user = userDAS.find(accessedObject.getOwningUserId());
            if (user != null) {
                validateCompany(accessedObject, user.getCompany().getId(), validatorType);
                validateUser(accessedObject, user.getId(), user.getCompany().getId(), validatorType, allowAgent);
            } else {
                // impossible, a persisted user must belong to a company
                String message = String.format("User " + accessedObject.getOwningUserId() + " does not belong to an entity.");
                LOG.warn(message);
                throw new SecurityException(message);
            }
        } else {
            LOG.warn("Data accessed via web-service call belongs to a transient user.");
        }
    }

    private CompanyUserDetails getPrincipal () {
        return (CompanyUserDetails) (springSecurityService.getPrincipal());
    }

    /**
     * Validates that if 'selected' customer can be edited by the partner associated to the provided 'owningUserId'
     *
     * @param selected
     */
    public boolean validateCustomerAgentRelationship(Integer owningUserId, UserDTO selected, Validator.Type validatorType) {
        if(null!=selected) {
            UserDTO user = userDAS.find(owningUserId);
            if(null!=user && user.getId()>0) {
                for(RoleDTO role : user.getRoles()) {
                    if(CommonConstants.TYPE_PARTNER.equals(role.getRoleTypeId())) {
                        PartnerBL partnerBL = new PartnerBL();
                        partnerBL.set(user.getPartner().getId());
                        PartnerDTO partner = partnerBL.getDTO();
                        if(!partner.getCustomers().contains(selected.getCustomer())) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
    
}