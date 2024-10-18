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

/*
 * Created on Mar 26, 2004
 */
package com.sapienter.jbilling.server.process;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDAS;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.process.task.BasicAgeingTask;
import com.sapienter.jbilling.server.process.task.IAgeingTask;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import com.sapienter.jbilling.server.util.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.NamingException;

import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emil
 */
public class AgeingBL {
    private static final Logger logger = LoggerFactory.getLogger(AgeingBL.class);

    private AgeingEntityStepDAS ageingDas = null;
    private AgeingEntityStepDTO ageing = null;

    public AgeingBL(Integer ageingId) {
        init();
        set(ageingId);
    }

    public AgeingBL() {
        init();
    }

    private void init() {
        ageingDas = new AgeingEntityStepDAS();
    }

    public AgeingEntityStepDTO getEntity() {
        return ageing;
    }

    public void set(Integer id) {
        ageing = ageingDas.find(id);
    }

    public ScrollableResults getUsersForAgeing(Integer entityId, Date ageingDate) {
        try {
            PluggableTaskManager<IAgeingTask> taskManager = new PluggableTaskManager<>(entityId, Constants.PLUGGABLE_TASK_AGEING);

            IAgeingTask task = taskManager.getNextClass();
            // If one was not configured just use the basic task by default
            if (task == null) {
                task = new BasicAgeingTask();
            }
            return task.findUsersToAge(entityId, ageingDate);

        } catch (PluggableTaskException e) {
            throw new SessionInternalError("Ageing task exception while running ageing review.", e);
        }
    }

    public List<InvoiceDTO> reviewUserForAgeing(Integer entityId, Integer userId, Date today) {
        try {
            PluggableTaskManager<IAgeingTask> taskManager = new PluggableTaskManager<>(entityId, Constants.PLUGGABLE_TASK_AGEING);

            CompanyDTO company = new EntityBL(entityId).getEntity();

            IAgeingTask task = taskManager.getNextClass();
            List<InvoiceDTO> overdueInvoices = new ArrayList<>();

            while (task != null) {
                overdueInvoices.addAll(task.reviewUser(entityId, company.getAgeingEntitySteps(), userId, today, null, Boolean.FALSE));
                task = taskManager.getNextClass();
            }

            return overdueInvoices;

        } catch (PluggableTaskException e) {
            throw new SessionInternalError("Ageing task exception while running ageing review.", e);
        }
    }

    public void out(UserDTO user, Integer excludedInvoiceId, Date effectiveDate) {
        try {
            PluggableTaskManager<IAgeingTask> taskManager = new PluggableTaskManager<>(user.getCompany().getId(), Constants.PLUGGABLE_TASK_AGEING);

            IAgeingTask task = taskManager.getNextClass();
            while (task != null) {
                task.removeUser(user, excludedInvoiceId, null, effectiveDate);
                task = taskManager.getNextClass();
            }

        } catch (PluggableTaskException e) {
            throw new SessionInternalError("Ageing task exception when removing user from ageing.", e);
        }
    }

    public void setUserStatus(Integer executorId, Integer userId, Integer statusId, Date today) {
        UserDTO user = new UserDAS().find(userId);
        UserStatusDTO userStatus = new UserStatusDAS().find(statusId);

        try {
            PluggableTaskManager<IAgeingTask> taskManager = new PluggableTaskManager<>(user.getCompany().getId(), Constants.PLUGGABLE_TASK_AGEING);

            IAgeingTask task = taskManager.getNextClass();
            while (task != null) {
                task.setUserStatus(user, userStatus, today, executorId);
                task = taskManager.getNextClass();
            }

        } catch (PluggableTaskException e) {
            throw new SessionInternalError("Ageing task exception when setting user status.", e);
        }
    }

    public AgeingDTOEx[] getOrderedSteps(Integer entityId) {
    	List<AgeingDTOEx> ageingDTOExs = new ArrayList<>();
    	for(CollectionType collectionType : CollectionType.values()){
    		AgeingDTOEx[] ageingDTOExArray = getSteps(entityId, null, null, collectionType);
            Collections.addAll(ageingDTOExs, ageingDTOExArray);
    	}

    	return ageingDTOExs.toArray(new AgeingDTOEx[ageingDTOExs.size()]);
    }

    public AgeingDTOEx[] getOrderedSteps(Integer entityId, CollectionType collectionType) {
        return getSteps(entityId, null, null, collectionType);
    }

    public AgeingDTOEx[] getSteps(Integer entityId,
                                  Integer executorLanguageId, Integer languageId, CollectionType collectionType) {
    	List<AgeingEntityStepDTO> ageingSteps = ageingDas.findAgeingStepsForEntity(entityId,collectionType);

    	AgeingDTOEx[] result  = new AgeingDTOEx[ageingSteps.size()];

        for (int i = 0; i < ageingSteps.size(); i++) {
            AgeingEntityStepDTO step = ageingSteps.get(i);
            AgeingDTOEx newStep = new AgeingDTOEx();
            newStep.setStatusId(step.getUserStatus().getId());
            UserStatusDTO statusRow = new UserStatusDAS().find(newStep.getStatusId());
            if (executorLanguageId != null) {
                newStep.setStatusStr(statusRow.getDescription(executorLanguageId));
            } else {
                newStep.setStatusStr(statusRow.getDescription());
            }
            newStep.setId(step.getId());

            newStep.setDays(step.getDays());
            newStep.setSuspend(step.getSuspend());
            newStep.setRetryPayment(step.getRetryPayment());
            newStep.setSendNotification(step.getSendNotification());
            newStep.setStopActivationOnPayment(step.getStopActivationOnPayment());
            newStep.setCanLogin(statusRow.getCanLogin());
            newStep.setDays(step.getDays());
            if (languageId != null) {
                newStep.setDescription(step.getDescription(languageId));
            } else {
                newStep.setDescription(step.getDescription());
            }

            newStep.setInUse(ageingDas.isAgeingStepInUse(step.getId()));

            result[i] = newStep;
        }

        return result;
    }

    public void setSteps(Integer entityId, Integer languageId, AgeingDTOEx[] steps, CollectionType collectionType) throws NamingException {
        logger.debug("Setting a total of {} steps", steps.length);
        List<String> errors = new ArrayList<>();
        List<Integer> stepList = Arrays.stream(steps)
                                       .filter(step -> step.getStatusId() != null)
                                       .map(AgeingDTOEx::getStatusId)
                                       .collect(Collectors.toList());

        //validate unique days into the steps
        long count = Arrays.stream(steps)
                           .map(AgeingDTOEx::getDays)
                           .distinct()
                           .count();

        if (count != steps.length) {
            logger.debug("Received non-unique days ageing step(s)");
            errors.add("config.ageing.error.non.unique.steps");
        }

        //validate unique names into the steps
        count = Arrays.stream(steps)
                      .map(AgeingDTOEx::getStatusStr)
                      .distinct()
                      .count();

        if (count != steps.length) {
            logger.debug("Received non-unique names ageing step(s)");
            errors.add("config.ageing.error.non.unique.steps.name");
        }

        //validate non negative days into the steps
        boolean hasNegativeNumbers = Arrays.stream(steps).anyMatch(step -> step.getDays() < 0);

        if (hasNegativeNumbers) {
            logger.debug("Received non-unique names ageing step(s)");
            errors.add("config.ageing.error.non.negative.days");
        }

        if (!errors.isEmpty()) {
            throw new SessionInternalError("Ageing steps bad formed", errors.toArray(new String[errors.size()]));
        }

        List<AgeingDTOEx> ageingSteps = Arrays.asList(getSteps(entityId, languageId, languageId, collectionType));
        List<AgeingDTOEx> existedAgeingSteps = ageingSteps.stream()
                                                          .filter(step -> stepList.contains(step.getStatusId()))
                                                          .collect(Collectors.toList());

        List<AgeingDTOEx> deletedAgeingSteps = ageingSteps.stream()
                                                          .filter(step -> !stepList.contains(step.getStatusId()))
                                                          .collect(Collectors.toList());

        deletedAgeingSteps.forEach( ageingStep -> {
            if (ageingStep.getInUse()) {
                throw new SessionInternalError("Ageing entity step is in use and can't be deleted!",
                        new String[]{ "config.ageing.error.steps.in.use" });
            }
            AgeingEntityStepDTO ageingDto = ageingDas.find(ageingStep.getId());
            UserStatusDAS userStatusDas = new UserStatusDAS();
            userStatusDas.delete(ageingDto.getUserStatus());
            ageingDas.delete(ageingDto);
            ageingDas.flush();
        });

        Arrays.stream(steps)
              .forEach(step -> {
                  logger.debug("Processing step for persisting: {}", step);

            AgeingDTOEx persistedStep = null;
            UserStatusDTO stepUserStatus = new UserStatusDAS().find(step.getStatusId());
            if (stepUserStatus != null && stepUserStatus.getAgeingEntityStep() != null) {
                persistedStep = existedAgeingSteps.stream()
                                                  .filter(stp -> stepUserStatus.getAgeingEntityStep().getId() == stp.getId())
                                                  .findFirst()
                                                  .orElse(null);
            }

            if (persistedStep != null) {
                existedAgeingSteps.remove(persistedStep);
                ageing = ageingDas.find(persistedStep.getId());
                // update
                logger.debug("Updating ageing step# {}", ageing.getId());
                ageing.setDays(step.getDays());
                ageing.setDescription(step.getStatusStr(), languageId);

                ageing.setSuspend(step.getSuspend());
                UserStatusDAS userDas = new UserStatusDAS();
                UserStatusDTO userStatusDTO = userDas.find(ageing.getUserStatus().getId());
                if (!userStatusDTO.getDescription(languageId).equals(ageing.getDescription(languageId))) {
                    logger.debug("Updating user status description to: {}", ageing.getDescription(languageId));
                    userStatusDTO.setDescription(ageing.getDescription(languageId), languageId);
                    userDas.save(userStatusDTO);
                }
                ageing.setRetryPayment(step.getRetryPayment());
                ageing.setSendNotification(step.getSendNotification());
                ageing.setStopActivationOnPayment(step.getStopActivationOnPayment());

            } else {
                logger.debug("Creating step.");
                ageingDas.create(entityId, step.getStatusStr(),
                        languageId, step.getDays(), step.getSendNotification(),
                        step.getRetryPayment(), step.getSuspend(),
                        step.getCollectionType(),
                        step.getStopActivationOnPayment()
                );
            }
        });
    }

    public AgeingWS getWS(AgeingDTOEx dto) {
    	if(null == dto) return null;

		AgeingWS ws = new AgeingWS();
		ws.setStatusId(dto.getStatusId());
		ws.setStatusStr(dto.getStatusStr());
		ws.setSuspended(dto.getSuspend() == 1);
		ws.setPaymentRetry(dto.getRetryPayment() == 1);
		ws.setSendNotification(dto.getSendNotification() == 1);
		ws.setInUse(dto.getInUse());
		ws.setDays(dto.getDays());
		ws.setEntityId((null != dto.getCompany()) ? dto.getCompany().getId() : null);
		ws.setStopActivationOnPayment(dto.getStopActivationOnPayment() == 1);
		ws.setCollectionType(dto.getCollectionType());
		return ws;
    }

    public AgeingDTOEx getDTOEx(AgeingWS ws) {
        AgeingDTOEx dto= new AgeingDTOEx();
        dto.setStatusId(ws.getStatusId());
        dto.setStatusStr(ws.getStatusStr());
        dto.setSuspend(ws.getSuspended() ? 1 : 0);
        dto.setSendNotification(ws.getSendNotification() ? 1 : 0);
        dto.setRetryPayment(ws.getPaymentRetry() ? 1 : 0);
        dto.setInUse(ws.getInUse());
        dto.setDays(null == ws.getDays() ? 0 : ws.getDays());
        dto.setStopActivationOnPayment(ws.getStopActivationOnPayment() ? 1 : 0);
        dto.setCollectionType(ws.getCollectionType());
        return dto;
    }
}