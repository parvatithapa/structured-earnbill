package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationMediumType;
import com.sapienter.jbilling.server.notification.db.*;
import com.sapienter.jbilling.server.util.db.NotificationCategoryDAS;
import com.sapienter.jbilling.server.util.db.NotificationCategoryDTO;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vivek on 31/10/14.
 */
public class NotificationMessageCopyTask extends AbstractCopyTask {

    NotificationMessageDAS notificationMessageDAS = null;
    NotificationMessageTypeDAS notificationMessageTypeDAS = null;
    NotificationCategoryDAS notificationCategoryDAS = null;

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NotificationMessageCopyTask.class));

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        LOG.debug("It has not been implemented yet. Remove this message after implementation");
        return false;
    }

    public NotificationMessageCopyTask() {
        init();
    }

    private void init() {
        notificationCategoryDAS = new NotificationCategoryDAS();
        notificationMessageDAS = new NotificationMessageDAS();
        notificationMessageTypeDAS = new NotificationMessageTypeDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create NotificationMessageCopyTask");
        copyNotificationMessage(entityId, targetEntityId);
    }

    private void copyNotificationMessage(int entityId, int targetEntityId) {
        NotificationMessageLineDAS messageLineHome = new NotificationMessageLineDAS();
        NotificationMessageSectionDAS messageSectionHome = new NotificationMessageSectionDAS();

        for (NotificationMessageDTO notificationMessageDTO : notificationMessageDAS.findByCompanyId(entityId)) {
            NotificationMessageDTO copyNotificationMessageDTO = notificationMessageDAS.create(notificationMessageDTO.getNotificationMessageType().getId(), targetEntityId, notificationMessageDTO.getLanguage().getId(), notificationMessageDTO.getUseFlag() == 0 ? false : true);

            copyNotificationMessageDTO.setAttachmentDesign(notificationMessageDTO.getAttachmentDesign());
            copyNotificationMessageDTO.setAttachmentType(notificationMessageDTO.getAttachmentType());
            copyNotificationMessageDTO.setIncludeAttachment(notificationMessageDTO.getIncludeAttachment());
            copyNotificationMessageDTO.setLanguage(notificationMessageDTO.getLanguage());

            List<NotificationMediumType> copyNmt = new ArrayList<NotificationMediumType>();
            if (notificationMessageDTO.getMediumTypes() != null)

                for (NotificationMediumType nmt : notificationMessageDTO.getMediumTypes()) {
                    copyNmt.add(nmt);
                }
            copyNotificationMessageDTO.setMediumTypes(copyNmt);
            copyNotificationMessageDTO.setNotifyAdmin(notificationMessageDTO.getNotifyAdmin());
            copyNotificationMessageDTO.setNotifyAllParents(notificationMessageDTO.getNotifyAllParents());
            copyNotificationMessageDTO.setNotifyParent(notificationMessageDTO.getNotifyParent());
            copyNotificationMessageDTO.setNotifyPartner(notificationMessageDTO.getNotifyPartner());
            copyNotificationMessageDTO.setUseFlag(notificationMessageDTO.getUseFlag());

            for (NotificationMessageSectionDTO nms : notificationMessageDTO.getNotificationMessageSections()) {
                NotificationMessageSectionDTO sectionDTO = messageSectionHome.create(nms.getSection());

                for (NotificationMessageLineDTO nml : nms.getNotificationMessageLines()) {
                    NotificationMessageLineDTO copyNML = messageLineHome.create(nml.getContent());
                    copyNML.setNotificationMessageSection(sectionDTO);
                    sectionDTO.getNotificationMessageLines().add(copyNML);
                }
                sectionDTO.setNotificationMessage(copyNotificationMessageDTO);
                copyNotificationMessageDTO.getNotificationMessageSections().add(sectionDTO);
            }
            notificationMessageDAS.save(copyNotificationMessageDTO);
        }
        LOG.debug("NotificationMessageCopyTask has been completed.");
    }
}
