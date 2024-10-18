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
import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.STR;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang.StringUtils;

import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.payment.IExternalCreditCardStorage;
import com.sapienter.jbilling.server.payment.IExternalACHStorage;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.event.AchDeleteEvent;
import com.sapienter.jbilling.server.user.event.AchUpdateEvent;

/**
 * @author Brian Cowdery
 * @since 14-09-2010
 */
public class SaveACHExternallyTask extends PluggableTask implements IInternalEventsTask {
    private static final Logger logger = LoggerFactory.getLogger(SaveACHExternallyTask.class);

    private static final ParameterDescription PARAM_CONTACT_TYPE = new ParameterDescription("contactType", false, INT);
    private static final ParameterDescription PARAM_EXTERNAL_SAVING_PLUGIN_ID = new ParameterDescription("externalSavingPluginId", true, STR);
    private static final ParameterDescription PARAM_OBSCURE_ON_FAIL = new ParameterDescription("obscureOnFail", false, BOOLEAN);

    private static final boolean DEFAULT_OBSCURE_ON_FAIL = false;

    //initializer for pluggable params
    {
    	descriptions.add(PARAM_CONTACT_TYPE);
        descriptions.add(PARAM_EXTERNAL_SAVING_PLUGIN_ID);
        descriptions.add(PARAM_OBSCURE_ON_FAIL);
    }

    private Integer contactType;
    private Integer[] externalSavingPluginIds;

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
            AchUpdateEvent.class,
            AchDeleteEvent.class
    };

    public Class<Event>[] getSubscribedEvents() { return events; }

    /**
     * Returns the configured contact type as an integer.
     *
     * @return contact type
     * @throws com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException if type cannot be converted to an integer
     */
    public Integer getContactType() throws PluggableTaskException {
        if (contactType == null) {
            try {
                if (parameters.get(PARAM_CONTACT_TYPE.getName()) == null) {
                    contactType = 1; // default if not configured
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
     * Returns the configured external saving event plugin ids ({@link IExternalCreditCardStorage})
     * as comma separated integer values.
     *
     * @return plugin ids of the configured external saving event plugin
     * @throws PluggableTaskException if id cannot be converted to an integer
     */
    public Integer[] getExternalSavingPluginIds() throws PluggableTaskException {
        String externalSavingPlugins = parameters.get(PARAM_EXTERNAL_SAVING_PLUGIN_ID.getName());
        if(StringUtils.isNotBlank(externalSavingPlugins)) {
            if(externalSavingPlugins.contains(",")) {
                externalSavingPluginIds = Arrays.stream(externalSavingPlugins.split(",")).map(x -> Integer.parseInt(x.trim())).collect(Collectors.toList()).toArray(new Integer[0]);
            } else {
                externalSavingPluginIds = new Integer[]{Integer.parseInt(externalSavingPlugins.trim())};
            }
        }
        return externalSavingPluginIds;
    }


    /**
     * @see IInternalEventsTask#process(com.sapienter.jbilling.server.system.event.Event)
     *
     * @param event event to process
     * @throws PluggableTaskException
     */
    public void process(Event event) throws PluggableTaskException {
        Integer[] extSavingPluginIds = getExternalSavingPluginIds();
        int count = 0;
        boolean isGatewayKeyGenerated = false;
        boolean areAllPluginsProcessed = false;
        for(Integer extSavingPluginId : extSavingPluginIds) {
            if(isGatewayKeyGenerated) {
                logger.debug("Gateway key is already generated hence skipping the plugin id : {}",extSavingPluginId);
                return;
            }
            ++count;
            areAllPluginsProcessed = Objects.equals(count, extSavingPluginIds.length);
            logger.debug("Processing plugin id : {}",extSavingPluginId);
            PluggableTaskBL<IExternalACHStorage> ptbl = new PluggableTaskBL<IExternalACHStorage>(extSavingPluginId);
            IExternalACHStorage externalCCStorage = ptbl.instantiateTask();
            if (event instanceof AchUpdateEvent) {
                logger.debug("Processing AchUpdateEvent ...");
                AchUpdateEvent ev = (AchUpdateEvent) event;
                String gateWayKey = externalCCStorage.storeACH(null, ev.getAch(), false);
                isGatewayKeyGenerated = updateAch(ev.getAch(), gateWayKey, areAllPluginsProcessed);
            } else if (event instanceof AchDeleteEvent) {
                logger.debug("Processing AchDeleteEvent ...");
                AchDeleteEvent ev = (AchDeleteEvent) event;
                String gateWayKey = externalCCStorage.deleteACH(null, ev.getAch());
                deleteAch(ev.getAch(), (gateWayKey==null)?null:gateWayKey.toCharArray());
            } else {
                throw new PluggableTaskException("Cant not process event " + event);
            }
        }
    }

    /**
     * Update the ACH object with the given gateway key.
     *
     * @param ach - ACH object to update
     * @param gatewayKey gateway key from external storage, null if storage failed.
     */
    private boolean updateAch(PaymentInformationDTO ach, String gatewayKey, boolean areAllPluginsProcessed) {
        PaymentInformationBL piBl = new PaymentInformationBL();
        if (gatewayKey != null) {
            logger.debug("Storing ach gateway key: " + gatewayKey);
            piBl.updateCharMetaField(ach, gatewayKey.toCharArray(), MetaFieldType.GATEWAY_KEY);
            piBl.obscureBankAccountNumber(ach);
            new PaymentInformationDAS().makePersistent(ach);
            return true;
        } else {
            if(areAllPluginsProcessed){
                if (getParameter(PARAM_OBSCURE_ON_FAIL.getName(), DEFAULT_OBSCURE_ON_FAIL)) {
                    piBl.obscureBankAccountNumber(ach);
                    new PaymentInformationDAS().makePersistent(ach);
                    logger.warn("gateway key returned from external store is null, obscuring ach with no key");
                } else {
                    logger.warn("gateway key returned from external store is null, ach will not be obscured!");
                }
            }
        }
        return false;
    }

    /**
     * Delete the ACH Object
     * @param ach
     * @param gatewayKey
     */
    private void deleteAch(PaymentInformationDTO ach, char[] gatewayKey) {
        if (gatewayKey == null || gatewayKey.length == 0) {
            logger.debug("Failed to delete the ACH Record - gateway key returned null." );
        }
    }
}
