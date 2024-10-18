package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentAgentWS;
import com.sapienter.jbilling.server.customerEnrollment.event.AcceptedByLDCEnrollmentEvent;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.*;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.partner.PartnerBL;
import com.sapienter.jbilling.server.user.partner.PartnerType;
import com.sapienter.jbilling.server.user.partner.PartnerWS;
import com.sapienter.jbilling.server.user.partner.db.CustomerCommissionDefinitionDTO;
import com.sapienter.jbilling.server.user.partner.db.CustomerCommissionDefinitionPK;
import com.sapienter.jbilling.server.user.partner.db.PartnerDAS;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.util.Constants;
import org.apache.log4j.Logger;

import java.math.BigDecimal;

/**
 * NGES specific.
 * The task will link new customers to their partners (brokers). If the broker does not exist one will be created.
 * The task executes for every customer when their enrollment is successfull.
 */
public class NewCustomerBrokerTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NewCustomerBrokerTask.class));

    private static final Class<Event> events[] = new Class[]{
            AcceptedByLDCEnrollmentEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event inEvent) throws PluggableTaskException {
        AcceptedByLDCEnrollmentEvent event = (AcceptedByLDCEnrollmentEvent)inEvent;

        //if there are broker ids we have to link the customer to the partner
        CustomerEnrollmentAgentWS[] agentWSes = event.getCustomerEnrollment().getCustomerEnrollmentAgents();
        if(agentWSes != null && agentWSes.length > 0) {
            UserBL userBL = new UserBL(event.getCustomerEnrollment().getCustomerId());
            CustomerDAS customerDAS = new CustomerDAS();
            CustomerDTO customer = userBL.getEntity().getCustomer();
            PartnerDAS partnerDAS = new PartnerDAS();
            for(CustomerEnrollmentAgentWS agentWS : agentWSes) {
                String brokerId = agentWS.getBrokerId();
                PartnerDTO partner = null;
                if(brokerId != null && brokerId.trim().length() > 0) {
                    partner = findOrCreatePartner(brokerId, partnerDAS);
                } else if(agentWS.getPartnerId() != null && agentWS.getPartnerId().intValue() > 0) {
                    partner = partnerDAS.find(agentWS.getPartnerId());
                }
                LOG.debug("Linking broker [%s] to customer [%s]", partner.getId(), customer.getId());
                customer.getPartners().add(partner);

                if(agentWS.getRate() != null && agentWS.getRate().trim().length() > 0) {
                    BigDecimal rate = new BigDecimal(agentWS.getRate());
                    CustomerCommissionDefinitionDTO commission = new CustomerCommissionDefinitionDTO(
                            new CustomerCommissionDefinitionPK(partner, customer.getBaseUser()), rate
                    );
                    customer.getBaseUser().getCommissionDefinitions().add(commission);
                }
            }
        }
    }

    /**
     * Load the partner with the given brokerId or create a new one with default values.
     * @param brokerId
     * @return
     */
    private PartnerDTO findOrCreatePartner(String brokerId, PartnerDAS partnerDAS) {
        PartnerDTO partnerDTO = partnerDAS.findForBrokerId(brokerId, getEntityId());
        if(partnerDTO != null) {
            return partnerDTO;
        }

        CompanyDTO entity = new CompanyDAS().find(getEntityId());

        UserWS user = createUserForPartner(brokerId, entity);
        PartnerWS partner = new PartnerWS();
        partner.setType(PartnerType.STANDARD.name());

        LOG.debug("Creating partner: %s \n  user: %s", partner, user);
        int partnerId = createPartner(user, partner, getEntityId());

        return partnerDAS.find(partnerId);
    }

    public Integer createPartner(UserWS newUser, PartnerWS partner, Integer entityId)
            throws SessionInternalError {
        UserBL bl = new UserBL();
        newUser.setUserId(0);

        if (bl.exists(newUser.getUserName(), entityId)) {
            throw new SessionInternalError(
                    "User already exists with username "
                            + newUser.getUserName(),
                    new String[] { "UserWS,userName,validation.error.user.already.exists" });
        }

        PartnerDTO partnerDto = PartnerBL.getPartnerDTO(partner);
        MetaFieldBL.fillMetaFieldsFromWS(entityId, partnerDto,
                newUser.getMetaFields());

        UserDTOEx dto = new UserDTOEx(newUser, entityId);
        dto.setPartner(partnerDto);

        Integer userId = bl.create(dto, null);

        ContactBL cBl = new ContactBL();
        if (newUser.getContact() != null) {
            newUser.getContact().setId(0);
            cBl.createForUser(new ContactDTOEx(newUser.getContact()), userId, null);
        }

        bl.createCredentialsFromDTO(dto);

        return bl.getDto().getPartner().getId();
    }

    /**
     * Create a user with default values for a new partner.
     * @param brokerId
     * @param entity
     * @return
     */
    private UserWS createUserForPartner(String brokerId, CompanyDTO entity) {
        UserWS user = new UserWS();
        user.setUserName("partner-" + entity.getId()+ "-" + brokerId);
        user.setCreateCredentials(false);
        user.setLanguageId(entity.getLanguageId());
        user.setCurrencyId(entity.getCurrencyId());
        user.setMainRoleId(Constants.TYPE_PARTNER);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        ContactWS contact = new ContactWS();
        contact.setEmail(user.getUserName() + "@entity"+entity.getId()+".com");
        contact.setFirstName("First Name");
        contact.setLastName("Last Name");
        user.setContact(contact);

        return user;
    }
}
