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

package com.sapienter.jbilling.server.payment.tasks;

import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.BOOLEAN;
import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.INT;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.payment.IExternalCreditCardStorage;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.event.NewContactEvent;
import com.sapienter.jbilling.server.user.event.NewCreditCardEvent;

/**
 * Internal Event Plugin designed to save a unique "gateway key" returned by the configured IExternalCreditCardStorage
 * task instead of the complete credit card number.
 *
 * This plugin subscribes to both NewCreditCardEvent and NewContactEvent. For NewContactEvent, this plugin
 * will only invoke the external save logic for new contacts matching the configured "contactType" id.
 */
public class SaveCreditCardExternallyTask extends PluggableTask implements IInternalEventsTask {
    private static final Logger logger = LoggerFactory.getLogger(SaveCreditCardExternallyTask.class);

    private static final ParameterDescription PARAM_CONTACT_TYPE = new ParameterDescription("contactType", true, INT);
    private static final ParameterDescription PARAM_EXTERNAL_SAVING_PLUGIN_ID = new ParameterDescription("externalSavingPluginId", true, INT);
    private static final ParameterDescription PARAM_REMOVE_ON_FAIL = new ParameterDescription("removeOnFail", false, BOOLEAN);
    private static final ParameterDescription PARAM_OBSCURE_ON_FAIL = new ParameterDescription("obscureOnFail", false, BOOLEAN);

    //initializer for pluggable params
    {
        descriptions.add(PARAM_CONTACT_TYPE);
        descriptions.add(PARAM_EXTERNAL_SAVING_PLUGIN_ID);
        descriptions.add(PARAM_OBSCURE_ON_FAIL);
        descriptions.add(PARAM_REMOVE_ON_FAIL);
    }

    private static final boolean DEFAULT_REMOVE_ON_FAIL = false;
    private static final boolean DEFAULT_OBSCURE_ON_FAIL = false;

    private Integer contactType;
    private Integer externalSavingPluginId;

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
        NewCreditCardEvent.class,
        NewContactEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() { return events; }

    // fixme: user parameters always come out as strings, int/float only available through db configured plugins

    /**
     * WARN: this parameter was used to compare against the
     * user's contact type. But, since now we do now have contact
     * types we use this parameter to compare against AIT id.
     *
     * Returns the configured contact type as an integer.
     *
     * @return contact type
     * @throws PluggableTaskException if type cannot be converted to an integer
     */
    public Integer getContactType() throws PluggableTaskException {
        if (contactType == null) {
            try {
                if (parameters.get(PARAM_CONTACT_TYPE.getName()) == null) {
                    contactType = -1; // default if not configured
                } else {
                    contactType = Integer.parseInt(parameters.get(PARAM_CONTACT_TYPE.getName()));
                }
            } catch (NumberFormatException e) {
                throw new PluggableTaskException("Configured contactType must be an integer!", e);
            }
        }
        return contactType;
    }

    /**
     * Returns the configured external saving event plugin id ({@link IExternalCreditCardStorage})
     * as an integer.
     *
     * @return plugin id of the configured external saving event plugin
     * @throws PluggableTaskException if id cannot be converted to an integer
     */
    public Integer getExternalSavingPluginId() throws PluggableTaskException {
        if (externalSavingPluginId == null) {
            try {
                externalSavingPluginId = Integer.parseInt(parameters.get(PARAM_EXTERNAL_SAVING_PLUGIN_ID.getName()));
            } catch (NumberFormatException e) {
                throw new PluggableTaskException("Configured externalSavingPluginId must be an integer!", e);
            }
        }
        return externalSavingPluginId;
    }

    /**
     * @see IInternalEventsTask#process(com.sapienter.jbilling.server.system.event.Event)
     *
     * @param event event to process
     * @throws PluggableTaskException
     */
    @Override
    public void process(Event event) throws PluggableTaskException {
        PluggableTaskBL<IExternalCreditCardStorage> ptbl = new PluggableTaskBL<>(getExternalSavingPluginId());
        IExternalCreditCardStorage externalCCStorage = ptbl.instantiateTask();

        if (event instanceof NewCreditCardEvent) {
            logger.debug("Processing NewCreditCardEvent ...");
            NewCreditCardEvent ev = (NewCreditCardEvent) event;

            if (new PaymentInformationBL().isBTPayment(ev.getCreditCard())) {
                logger.debug("Payment Instrument has BT ID - can't save through gateway.");
            } else {
                // only save credit cards associated with users
                if (ev.getCreditCard().getUser() != null) {
                    String gateWayKey = externalCCStorage.storeCreditCard(null, ev.getCreditCard());
                    updateCreditCard(ev.getCreditCard(), gateWayKey);
                } else if(ev.getUserId() != null) {
                    ContactDTO contact = new UserBL(ev.getUserId()).getEntity().getContact();
                    PaymentInformationDTO paymentInformationDTO = ev.getCreditCard();
                    paymentInformationDTO.setUser(new UserDAS().findNow(ev.getUserId()));
                    String gateWayKey = externalCCStorage.storeCreditCard(contact, ev.getCreditCard());
                    paymentInformationDTO.setUser(null);
                    updateCreditCard(ev.getCreditCard(), gateWayKey);
                } else {
                    logger.debug("Credit card is not associated with a user (card for payment) - can't save through gateway.");
                }
            }

        } else if (event instanceof NewContactEvent) {
            logger.debug("Processing NewContactEvent ...");
            NewContactEvent ev = (NewContactEvent) event;

            Integer userId = ev.getUserId();
            UserDTO user = new UserDAS().find(userId);

            if(null != user.getCustomer()){
                Integer groupId = ev.getGroupId();
                logger.debug("Group Id: {}, plug-in expects: {}",groupId, getContactType());

                if ((null == groupId && null != ev.getContactDto()) || (getContactType() == groupId)) {
                    ContactDTO contact = null;

                    if(null != groupId) {
                        contact = ContactBL.buildFromMetaField(userId, groupId, companyCurrentDate());
                    } else {
                        contact = ev.getContactDto();
                    }

                    UserBL userBl = new UserBL(contact.getUserId());
                    List<PaymentInformationDTO> creditCards = userBl.getAllCreditCards();

                    if (creditCards != null) {
                        // credit card has changed or was not previously obscured
                        for(PaymentInformationDTO creditCard : creditCards) {
                            if(!new PaymentInformationBL().isBTPayment(creditCard)){
                                String gateWayKey = externalCCStorage.storeCreditCard(contact, creditCard);
                                updateCreditCard(creditCard, gateWayKey);
                            }
                        }
                    } else {
                        /*  call the external store without a credit card. It's possible the payment gateway
                            may have some vendor specific recovery facilities, or perhaps they operate on different
                            data? We'll leave it open ended so we don't restrict possible client implementations.
                         */
                        logger.warn("Cannot determine credit card for storage, invoking external store with contact only");
                        String gateWayKey = externalCCStorage.storeCreditCard(contact, null);
                        updateCreditCard(null, gateWayKey);
                    }
                }
            } else {
                logger.debug("The user is not customer. We do not store CC for non customer users");
            }
        } else {
            throw new PluggableTaskException("Cant not process event " + event);
        }
    }

    /**
     * Update the credit card object with the given gateway key. If the gateway key is null,
     * handle the external storage as a failure.
     *
     * If PARAM_OBSCURE_ON_FAIL is true, obscure the card number even if gateway key is null.
     * If PARAM_REMOVE_ON_FAIL is true, delete the credit card and remove from the user map if the gateway key is null.
     *
     * @param creditCard credit card to update
     * @param gatewayKey gateway key from external storage, null if storage failed.
     */
    private void updateCreditCard(PaymentInformationDTO creditCard, String gatewayKey) {
        PaymentInformationBL piBl= new PaymentInformationBL();
        if (gatewayKey != null) {
            logger.debug("Storing gateway key: {}", gatewayKey);
            piBl.updateCharMetaField(creditCard, gatewayKey.toCharArray(), MetaFieldType.GATEWAY_KEY);
            piBl.obscureCreditCardNumber(creditCard);
        } else {

            // obscure credit cards on failure, useful for clients who under no circumstances want a plan-text
            // card to be stored in the jBilling database
            if (getParameter(PARAM_OBSCURE_ON_FAIL.getName(), DEFAULT_OBSCURE_ON_FAIL)) {
                piBl.obscureCreditCardNumber(creditCard);
                logger.warn("gateway key returned from external store is null, obscuring credit card with no key");
            } else {
                logger.warn("gateway key returned from external store is null, credit card will not be obscured!");
            }

            // delete the credit card on failure so that it cannot be used for future payments. useful when
            // paired with PARAM_OBSCURE_ON_FAIL as it prevents accidental payments with invalid cards.
            if (getParameter(PARAM_REMOVE_ON_FAIL.getName(), DEFAULT_REMOVE_ON_FAIL)) {
                piBl.delete(creditCard.getId());;
                logger.warn("gateway key returned from external store is null, deleting card and removing from user map");
            }
        }
    }
}
