package com.sapienter.jbilling.server.notification.builder;

import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.MessageSection;
import com.sapienter.jbilling.server.notification.NotificationMediumType;
import com.sapienter.jbilling.server.util.Constants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sapienter.jbilling.common.Constants.D_METHOD_EMAIL;

/**
 * Created by leandro on 01/06/17.
 */
public class NotificationBuilder {

    private String attachmentDesign;
    private String attachmentFile;
    private String attachmentType;
    private MessageSection[] content;
    private Integer deliveryMethodId;
    private Integer includeAttachment;
    private Integer languageId;
    private List<NotificationMediumType> mediumTypes;
    private Integer notifyAdmin;
    private Integer notifyAllParents;
    private Integer notifyParent;
    private Integer notifyPartner;
    private Map<String, Object> parameters;
    private Integer typeId;
    private Boolean useFlag;

    public NotificationBuilder() {
        this.attachmentDesign = "";
        this.content = new MessageSection[0];
        this.deliveryMethodId = D_METHOD_EMAIL;
        this.includeAttachment = 0;
        this.languageId = Constants.LANGUAGE_ENGLISH_ID;
        this.mediumTypes = Arrays.asList(NotificationMediumType.values());
        this.notifyAdmin = 0;
        this.notifyPartner = 0;
        this.notifyParent = 0;
        this.notifyAllParents = 0;
        this.parameters = new HashMap();
        this.useFlag = Boolean.TRUE;
    }

    public NotificationBuilder withAttachmentDesign(String attachmentDesign) {
        this.attachmentDesign = attachmentDesign;
        return this;
    }

    public NotificationBuilder withAttachmentFile(String attachmentFile) {
        this.attachmentFile = attachmentFile;
        return this;
    }

    public NotificationBuilder withAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
        return this;
    }

    public NotificationBuilder withContent(MessageSection[] content) {
        this.content = content;
        return this;
    }

    public NotificationBuilder withDeliveryMethodId(Integer deliveryMethodId) {
        this.deliveryMethodId = deliveryMethodId;
        return this;
    }

    public NotificationBuilder withIncludeAttachment(Integer includeAttachment) {
        this.includeAttachment = includeAttachment;
        return this;
    }

    public NotificationBuilder withLanguageId(Integer languageId) {
        this.languageId = languageId;
        return this;
    }

    public NotificationBuilder withMediumTypes(List mediumTypes) {
        this.mediumTypes = mediumTypes;
        return this;
    }

    public NotificationBuilder addMediumType(NotificationMediumType notificationMediumType) {
        this.mediumTypes.add(notificationMediumType);
        return this;
    }

    public NotificationBuilder addAllMediumTypes(List<NotificationMediumType> mediumTypes) {
        this.mediumTypes.addAll(mediumTypes);
        return this;
    }

    public NotificationBuilder withNotifyAdmin(Integer notifyAdmin) {
        this.notifyAdmin = notifyAdmin;
        return this;
    }

    public NotificationBuilder withNotifyAllParents(Integer notifyAllParents) {
        this.notifyAllParents = notifyAllParents;
        return this;
    }

    public NotificationBuilder withNotifyPartner(Integer notifyPartner) {
        this.notifyPartner = notifyPartner;
        return this;
    }

    public NotificationBuilder withParameters(HashMap<String, Object> parameters) {
        this.parameters = parameters;
        return this;
    }

    public NotificationBuilder addParameter(String name, Object value){
        this.parameters.put(name, value);
        return this;
    }

    public NotificationBuilder addAllParameters(HashMap<String, Object> parameters) {
        this.parameters.putAll(parameters);
        return this;
    }

    public NotificationBuilder withTypeId(Integer typeId) {
        this.typeId = typeId;
        return this;
    }

    public NotificationBuilder withUseFlag(Boolean useFlag) {
        this.useFlag = useFlag;
        return this;
    }

    public MessageDTO build() {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setAttachmentDesign(attachmentDesign);
        messageDTO.setAttachmentFile(attachmentFile);
        messageDTO.setAttachmentType(attachmentType);
        messageDTO.setContent(content);
        messageDTO.setDeliveryMethodId(deliveryMethodId);
        messageDTO.setIncludeAttachment(includeAttachment);
        messageDTO.setLanguageId(languageId);
        messageDTO.setMediumTypes(mediumTypes);
        messageDTO.setNotifyAdmin(notifyAdmin);
        messageDTO.setNotifyAllParents(notifyAllParents);
        messageDTO.setNotifyParent(notifyParent);
        messageDTO.setNotifyPartner(notifyPartner);
        messageDTO.setTypeId(typeId);
        messageDTO.setUseFlag(useFlag);

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            messageDTO.addParameter(entry.getKey(), entry.getValue());
        }

        return messageDTO;
    }
}
