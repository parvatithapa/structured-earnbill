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

package com.sapienter.jbilling.server.customerEnrollment.db;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.account.AccountTypeBL;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentAgentWS;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentCommentWS;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentStatus;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.customerEnrollment.event.EnrollmentCompletionEvent;
import com.sapienter.jbilling.server.customerEnrollment.event.ValidateEnrollmentEvent;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.process.event.SureAddressEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.partner.db.PartnerDAS;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author Emil
 */
public class CustomerEnrollmentBL {

    private CustomerEnrollmentDTO customerEnrollmentDTO = null;
    private CustomerEnrollmentDAS customerEnrollmentDAS = null;
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CustomerEnrollmentBL.class));

    public CustomerEnrollmentBL(){
        init();
    }

    public CustomerEnrollmentBL(Integer enrollmentId){
        init();
        customerEnrollmentDAS.find(enrollmentId);
    }

    private  void init(){
        customerEnrollmentDAS=new CustomerEnrollmentDAS();
    }


    public CustomerEnrollmentWS getWS(CustomerEnrollmentDTO dto) {
        if (dto == null) {
            dto = this.customerEnrollmentDTO;
        }
        CustomerEnrollmentWS ws = new CustomerEnrollmentWS();
        ws.setId(dto.getId());
        ws.setAccountTypeId(dto.getAccountType().getId());
        ws.setDeleted(dto.getDeleted());
        ws.setBulkEnrollment(dto.isBulkEnrollment());
        ws.setCompanyName(dto.getCompany().getDescription());

        ws.setAccountTypeName(dto.getAccountType().getDescription());

        ws.setCreateDatetime(dto.getCreateDatetime());
        List <CustomerEnrollmentCommentWS> customerEnrollmentCommentWSList =new ArrayList<CustomerEnrollmentCommentWS>();

        CustomerEnrollmentCommentBL customerEnrollmentCommentBL=new CustomerEnrollmentCommentBL();

        if(dto.getComments()!=null){
            for(CustomerEnrollmentCommentDTO customerEnrollmentCommentDTO:dto.getComments()){
                customerEnrollmentCommentWSList.add(customerEnrollmentCommentBL.getWS(customerEnrollmentCommentDTO));
            }
        }
        ws.setCustomerEnrollmentComments(customerEnrollmentCommentWSList.toArray(new CustomerEnrollmentCommentWS[customerEnrollmentCommentWSList.size()]));

        List <CustomerEnrollmentAgentWS> customerEnrollmentAgentWSList =new ArrayList<CustomerEnrollmentAgentWS>();
        if(dto.getAgents() != null){
            for(CustomerEnrollmentAgentDTO customerEnrollmentAgentDTO:dto.getAgents()){
                customerEnrollmentAgentWSList.add(getCustomerEnrollmentAgentWS(customerEnrollmentAgentDTO));
            }
        }
        ws.setCustomerEnrollmentAgents(customerEnrollmentAgentWSList.toArray(new CustomerEnrollmentAgentWS[customerEnrollmentAgentWSList.size()]));

        ws.setMetaFields(convertCustomerEnrollmentMetaFieldsToWS(dto.getCompany().getId(), new AccountTypeBL(dto.getAccountType().getId()).getAccountType(), dto));
        ws.setAccountNumber((String) ws.getMetaFieldValue(FileConstants.CUSTOMER_ACCOUNT_KEY));
        ws.setEntityId(dto.getCompany().getId());
        ws.setStatus(dto.getStatus());
        if(dto.getUser() != null) {
            ws.setCustomerId(dto.getUser().getId());
        }

        if(dto.getParentCustomer()!=null){
            ws.setParentUserId(dto.getParentCustomer().getUserId());
        }
        if(dto.getParentEnrollment()!=null){
            ws.setParentEnrollmentId(dto.getParentEnrollment().getId());
        }
        ws.setMessage(dto.getMessage());
        return ws;
    }

    private CustomerEnrollmentAgentWS getCustomerEnrollmentAgentWS(CustomerEnrollmentAgentDTO agentDTO) {
        CustomerEnrollmentAgentWS ws = new CustomerEnrollmentAgentWS();
        ws.setBrokerId(agentDTO.getBrokerId());
        if(agentDTO.getRate() != null) {
            ws.setRate(agentDTO.getRate().toString());
        }
        if(agentDTO.getPartner() != null) {
            PartnerDTO partnerDTO = agentDTO.getPartner();
            ws.setPartnerId(partnerDTO.getId());
            String partnerName = (partnerDTO.getBaseUser().getContact().getFirstName() != null && partnerDTO.getBaseUser().getContact().getFirstName().trim().length() > 0) ?
                    (partnerDTO.getBaseUser().getContact().getFirstName() + ' ' + partnerDTO.getBaseUser().getContact().getLastName()) : partnerDTO.getBaseUser().getUserName();

            ws.setPartnerName(partnerName);
        } else if(agentDTO.getBrokerId() != null) {
            ws.setPartnerName(agentDTO.getBrokerId());
        }
        return ws;
    }

    public CustomerEnrollmentDTO getDTO(CustomerEnrollmentWS ws) throws SessionInternalError{

        CustomerEnrollmentDTO dto = new CustomerEnrollmentDTO();
        if(ws.getId() > 0 ){
            dto.setId(ws.getId());
            dto.setVersionNum(customerEnrollmentDAS.findNow(ws.getId()).getVersionNum());
        }

        dto.setDeleted(ws.getDeleted());
        dto.setBulkEnrollment(ws.isBulkEnrollment());
        MetaFieldBL.fillMetaFieldsFromWS(ws.getEntityId(), dto, ws.getMetaFields());
        dto.setAccountType(new AccountTypeBL(ws.getAccountTypeId()).getAccountType());
        if(ws.getCustomerId() != null && ws.getCustomerId() > 0) {
            dto.setUser(new UserDAS().find(ws.getCustomerId()));
        }


        if(ws.getCustomerEnrollmentComments()!=null){
            CustomerEnrollmentCommentBL customerEnrollmentCommentBL=new CustomerEnrollmentCommentBL();
            for(CustomerEnrollmentCommentWS customerEnrollmentCommentWS:ws.getCustomerEnrollmentComments()){
                dto.getComments().add(customerEnrollmentCommentBL.getDTO(customerEnrollmentCommentWS));
            }
        }

        if(ws.getCustomerEnrollmentAgents()!=null){
            PartnerDAS partnerDAS = new PartnerDAS();
            for(CustomerEnrollmentAgentWS agentWS:ws.getCustomerEnrollmentAgents()){
                dto.getAgents().add(getCustomerEnrollmentAgent(agentWS, dto, partnerDAS));
            }
        }

        if(ws.getParentUserId()!=null){
            dto.setParentCustomer(new UserDAS().find(ws.getParentUserId()));
        }

        if(ws.getParentEnrollmentId()!=null){
            dto.setParentEnrollment(new CustomerEnrollmentDAS().find(ws.getParentEnrollmentId()));
        }
        dto.setCompany(new CompanyDAS().find(ws.getEntityId()));

        dto.setCreateDatetime(ws.getCreateDatetime());
        dto.setMetaFields(new LinkedList<MetaFieldValue>(dto.getMetaFields()));
        dto.setStatus(ws.getStatus());
        dto.setMessage(ws.getMessage());
        return dto;
    }

    private void updateComments(CustomerEnrollmentDTO targetDto, CustomerEnrollmentDTO sourceDto) {
        for(CustomerEnrollmentCommentDTO sourceComment: sourceDto.getComments()) {
            if(sourceComment.getId() == 0 ) {
                CustomerEnrollmentCommentDTO targetComment = new CustomerEnrollmentCommentDTO();
                targetComment.setComment(sourceComment.getComment());
                targetComment.setUser(sourceComment.getUser());
                targetComment.setCreationTime(sourceComment.getCreationTime());
                targetComment.setCustomerEnrollment(sourceDto);

                targetDto.getComments().add(targetComment);
            }
        }
    }

    private void updateAgents(CustomerEnrollmentDTO targetDto, CustomerEnrollmentDTO sourceDto) {
        List<CustomerEnrollmentAgentDTO> targetAgents = new ArrayList<>(targetDto.getAgents());
        List<CustomerEnrollmentAgentDTO> sourceAgents = new ArrayList<>(sourceDto.getAgents());

        int min = Math.min(targetAgents.size(), sourceAgents.size());

        //copy values
        for(int i=0; i<min; i++) {
            CustomerEnrollmentAgentDTO targetAgent = targetAgents.get(i);
            CustomerEnrollmentAgentDTO sourceAgent = sourceAgents.get(i);

            targetAgent.setRate(sourceAgent.getRate());
            targetAgent.setPartner(sourceAgent.getPartner());
            targetAgent.setBrokerId(sourceAgent.getBrokerId());
        }

        //remove agents that should be removed
        for(int i=min; i<targetAgents.size(); i++) {
            targetDto.getAgents().remove(targetAgents.get(i));
        }

        //add new agents
        for(int i=min; i<sourceAgents.size(); i++) {
            CustomerEnrollmentAgentDTO sourceAgent = sourceAgents.get(i);

            CustomerEnrollmentAgentDTO agent = new CustomerEnrollmentAgentDTO();
            agent.setRate(sourceAgent.getRate());
            agent.setPartner(sourceAgent.getPartner());
            agent.setBrokerId(sourceAgent.getBrokerId());
            agent.setEnrollment(targetDto);

            targetDto.getAgents().add(agent);
        }
       /* //create cache of current objects
        for(CustomerEnrollmentAgentDTO agentDTO : targetDto.getAgents()) {
            if(agentDTO.getPartner() != null) {
                agentDTOMap.put(agentDTO.getPartner().getId(), agentDTO);
            }
            if(agentDTO.getBrokerId() != null && !agentDTO.getBrokerId().isEmpty()) {
                brokerDTOMap.put(agentDTO.getBrokerId(), agentDTO);
            }
        }

        //update or create current agents.
        if(sourceDto.getAgents() != null) {
            for(CustomerEnrollmentAgentDTO agentDTO : sourceDto.getAgents()) {
                CustomerEnrollmentAgentDTO currAgentDTO = null;
                if(agentDTO.getPartner() != null) {
                    currAgentDTO = agentDTOMap.remove(agentDTO.getPartner().getId());
                    brokerDTOMap.remove(agentDTO.getBrokerId());
                } else if(agentDTO.getBrokerId() != null && agentDTO.getBrokerId().length() > 0) {
                    currAgentDTO = brokerDTOMap.remove(agentDTO.getBrokerId());
                }

                if(currAgentDTO == null) {
                    currAgentDTO = agentDTO;
                    currAgentDTO.setEnrollment(targetDto);
                    targetDto.getAgents().add(currAgentDTO);
                } else {
                    currAgentDTO.setRate(agentDTO.getRate());
                }
            }
        }

        //remove agents not in the list anymore
        for(CustomerEnrollmentAgentDTO agentDTO : agentDTOMap.values()) {
            targetDto.getAgents().remove(agentDTO);
        }

        for(CustomerEnrollmentAgentDTO agentDTO : brokerDTOMap.values()) {
            targetDto.getAgents().remove(agentDTO);
        }*/
    }

    private CustomerEnrollmentAgentDTO getCustomerEnrollmentAgent(CustomerEnrollmentAgentWS ws, CustomerEnrollmentDTO enrollmentDTO, PartnerDAS partnerDAS) {
        CustomerEnrollmentAgentDTO dto = new CustomerEnrollmentAgentDTO();
        dto.setBrokerId(ws.getBrokerId());
        dto.setEnrollment(enrollmentDTO);
        if(ws.getPartnerId() != null) {
            dto.setPartner(partnerDAS.find(ws.getPartnerId()));
        }

        if(ws.getRate() != null && ws.getRate().length() > 0) {
            try {
                dto.setRate(new BigDecimal(ws.getRate()));
            } catch (Exception e) {
                throw new SessionInternalError("CustomerEnrollmentAgentWS,rate,customer.enrollment.agent.rate.invalid");
            }
        }

        return dto;
    }

    public Integer save(CustomerEnrollmentDTO customerEnrollmentDTO) {
//        Set<CustomerEnrollmentCommentDTO> customerEnrollmentCommentDTOs=customerEnrollmentDTO.getComments();
        CustomerEnrollmentDTO customerEnrollment = null;

        if(customerEnrollmentDTO.getId() == 0) {
            customerEnrollment = customerEnrollmentDAS.save(customerEnrollmentDTO);
        } else {
            customerEnrollment = customerEnrollmentDAS.find(customerEnrollmentDTO.getId());
            customerEnrollment.setDeleted(customerEnrollmentDTO.getDeleted());
            customerEnrollment.setMessage(customerEnrollmentDTO.getMessage());
            customerEnrollment.setAccountType(customerEnrollmentDTO.getAccountType());
            customerEnrollment.setParentCustomer(customerEnrollmentDTO.getParentCustomer());
            customerEnrollment.setParentEnrollment(customerEnrollmentDTO.getParentEnrollment());
            customerEnrollment.setStatus(customerEnrollmentDTO.getStatus());
            customerEnrollment.setUser(customerEnrollmentDTO.getUser());

            updateAgents(customerEnrollment, customerEnrollmentDTO);

            updateComments(customerEnrollment, customerEnrollmentDTO);

            for (MetaFieldValue metaFieldValue : customerEnrollmentDTO.getMetaFields()) {
                customerEnrollment.setMetaField(metaFieldValue.getField(), metaFieldValue.getValue());
            }
        }

        LOG.debug("customer enrollment  has been saved.");
        if(customerEnrollment.getStatus()!=null && customerEnrollment.getStatus().equals(CustomerEnrollmentStatus.VALIDATED)) {
            EnrollmentCompletionEvent event = new EnrollmentCompletionEvent(customerEnrollment.getCompany().getId(), customerEnrollment.getId());
            EventManager.process(event);
        }

        return customerEnrollment.getId();
    }

    public void delete(Integer customerEnrollmentId){
        LOG.debug("customerEnrollmentDAS.countChildCompanies(customerEnrollmentId)   " + customerEnrollmentDAS.countChildCompanies(customerEnrollmentId));
        if(customerEnrollmentDAS.countChildCompanies(customerEnrollmentId) > 0) {
            throw new SessionInternalError(
                    "You can not delete this enrollment as it's all child did not enrolled.",
                    new String[] { "CustomerEnrollmentDTO,label,customer.enrollment.not.delete.label" });
        }
        CustomerEnrollmentDTO customerEnrollmentDTO=customerEnrollmentDAS.find(customerEnrollmentId);
        customerEnrollmentDTO.setDeleted(1);
        customerEnrollmentDAS.save(customerEnrollmentDTO);
    }

    public Long countByAccountType(Integer accountType){
        return customerEnrollmentDAS.countByAccountType(accountType);
    }

    public CustomerEnrollmentWS getCustomerEnrollmentWS(Integer customerEnrollmentId) {
        if (customerEnrollmentId == null) {
            return null;
        }
        CustomerEnrollmentDTO customerEnrollmentDTO = new CustomerEnrollmentDAS().findById(customerEnrollmentId);
        CustomerEnrollmentWS customerEnrollmentWS = null;
        if(customerEnrollmentDTO != null) {
            customerEnrollmentWS = getWS(customerEnrollmentDTO);
        }
        return customerEnrollmentWS;
    }
    public void validateEnrollment(CustomerEnrollmentDTO customerEnrollmentDTO) throws SessionInternalError{

        // calling sure adding plugin
        Integer entityId = (customerEnrollmentDTO.getCompany().getParent() != null) ? customerEnrollmentDTO.getCompany().getParent().getId() : customerEnrollmentDTO.getCompany().getId();
        EventManager.process(new SureAddressEvent(entityId, customerEnrollmentDTO));

        //validating metafield level side validtion
        Set<String> errorMessages = new HashSet<String>();
        for (MetaFieldValue metaFieldValue : customerEnrollmentDTO.getMetaFields()){
            try {
                MetaFieldBL.validateMetaField(customerEnrollmentDTO.getCompany().getLanguageId(), metaFieldValue.getField(), metaFieldValue, customerEnrollmentDTO);
            }
            catch (SessionInternalError e){
                Collections.addAll(errorMessages, e.getErrorMessages());
            }
        }


        //check that agents are only added once
        Set<String> agentIdSet = new HashSet<>();
        for(CustomerEnrollmentAgentDTO agentDTO : customerEnrollmentDTO.getAgents()) {
            StringBuilder key = new StringBuilder();
            if(agentDTO.getPartner() != null) {
                key.append("p").append(Integer.toString(agentDTO.getPartner().getId()));
            } else if(agentDTO.getBrokerId() != null) {
                key.append("b").append(agentDTO.getBrokerId().trim());
            }

            String keyString = key.toString();
            if(agentIdSet.contains(keyString)) {
               errorMessages.add("CustomerEnrollmentDTO,agent,customer.enrollment.agent.duplicate");
               break;
            }

            agentIdSet.add(keyString);
        }

        try{
            LOG.debug("triggering ValidateEnrollmentEvent even for NGES specific valiation");
            EventManager.process(new ValidateEnrollmentEvent(customerEnrollmentDTO.getCompany().getId(), customerEnrollmentDTO));
        }catch (SessionInternalError sie) {
            Collections.addAll(errorMessages, sie.getErrorMessages());
        }

        if (errorMessages.size() > 0) {
            throw new SessionInternalError(errorMessages.toArray(new String[errorMessages.size()]));
        }



        LOG.debug("Validating is any non drop customer/enrollment is exist in the system for the account type");
        EventManager.process(new ValidateEnrollmentEvent(customerEnrollmentDTO.getCompany().getId(), customerEnrollmentDTO));
    }


    public MetaFieldValueWS[] convertCustomerEnrollmentMetaFieldsToWS(Integer entityId, AccountTypeDTO accountType, CustomerEnrollmentDTO entity) {

        Set<AccountInformationTypeDTO> infoTypes = accountType.getInformationTypes();

        Set<MetaField> availableMetaFields=new HashSet<MetaField>();

        for(AccountInformationTypeDTO informationTypeDTO:infoTypes){
            availableMetaFields.addAll(informationTypeDTO.getMetaFields());
        }
        // binding enrollment specific metafield
        EntityType[] enrollmentEntityType=new EntityType[]{EntityType.ENROLLMENT};
        List<MetaField> enrollmentMetaFields=MetaFieldBL.getAvailableFieldsList(entity.getCompany().getId(), enrollmentEntityType);
        availableMetaFields.addAll(enrollmentMetaFields);
        //code for getting all ait metafield
        MetaFieldValueWS[] result = new MetaFieldValueWS[]{};
        if (availableMetaFields.size()>0) {
            result = new MetaFieldValueWS[availableMetaFields.size()];
            int i = 0;
            for (MetaField field : availableMetaFields) {
                Integer groupId=null;
                for(MetaFieldGroup metaFieldGroup:field.getMetaFieldGroups()){
                    if(metaFieldGroup.getEntityType()== EntityType.ACCOUNT_TYPE){
                        groupId=metaFieldGroup.getId();
                        break;
                    }
                }
                MetaFieldValue value = entity.getMetaField(field.getName(), groupId);
                if (value == null) {
                    value = field.createValue();
                }
                MetaFieldValueWS metaFieldValueWS = MetaFieldBL.getWS(value, groupId);
                result[i++] = metaFieldValueWS;
            }
        }
        return result;
    }

    /**
     * This method used for finding the enrollment id's which are not enrolled ByUsing ScrollableResult.
     *
     * @param entityId .
     * @return List<Integer> enrollment id's.
     */
    public List<Integer> findIdsByEntity(Integer entityId) {
        List<Integer> result = new CustomerEnrollmentDAS().findIdsByEntity(entityId);
        return result;
    }
}
