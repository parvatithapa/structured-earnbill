package com.sapienter.jbilling.server.user;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.io.Serializable;
import java.util.Date;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

@ApiModel(value = "Customer Note Data", description = "CustomerNoteWS Model")
public class CustomerNoteWS implements Serializable {
    private int noteId;
    @NotNull(message="validation.error.notnull")
    @Size(min= 1, max = 50, message = "validation.error.size,5,50")
    private String noteTitle;
    @NotNull(message="validation.error.notnull")
    @Size(min = 1, max = 6000, message = "validation.error.size,5,6000")
    private String noteContent;
    @ConvertToTimezone
    private Date creationTime;
    private Integer entityId;
    private Integer customerId;
    private Integer userId;
    private boolean notesInInvoice = false;

    public CustomerNoteWS() {
    }

    public CustomerNoteWS(int noteId, String noteTitle, String noteContent, Date creationTime, Integer entityId, Integer customerId, Integer userId) {
        this.noteId = noteId;
        this.noteTitle = noteTitle;
        this.noteContent = noteContent;
        this.creationTime = creationTime;
        this.entityId = entityId;
        this.customerId = customerId;
        this.userId = userId;
    }

    public CustomerNoteWS(int noteId, String noteTitle, String noteContent, Date creationTime, Integer entityId, Integer customerId, Integer userId,boolean notesInInvoice) {
        this.noteId = noteId;
        this.noteTitle = noteTitle;
        this.noteContent = noteContent;
        this.creationTime = creationTime;
        this.entityId = entityId;
        this.customerId = customerId;
        this.userId = userId;
        this.notesInInvoice = notesInInvoice;
    }

    @ApiModelProperty(value = "The id of the note entity")
    public int getNoteId() {
        return noteId;
    }

    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }

    @ApiModelProperty(value = "The title of the note", required = true)
    public String getNoteTitle() {
        return noteTitle;
    }

    public void setNoteTitle(String noteTitle) {
        this.noteTitle = noteTitle;
    }

    @ApiModelProperty(value = "The content of the note", required = true)
    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }

    @ApiModelProperty(value = "The date when this note was created")
    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    @ApiModelProperty(value = "The id of the company for which this note exists")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @ApiModelProperty(value = "The id of the user for which this note exists")
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @ApiModelProperty(value = "The id of the customer for which this note exists")
    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    @ApiModelProperty(value = "True - the customer notes will be included in the invoice; False - they will not be included")
    public boolean getNotesInInvoice() {
        return notesInInvoice;
    }

    public void setNotesInInvoice(boolean notesInInvoice) {
        this.notesInInvoice = notesInInvoice;
    }

    @Override
    public String toString() {
        return "CustomerNoteWS{" +
                "noteId=" + noteId +
                ", noteTitle='" + noteTitle + '\'' +
                ", noteContent='" + noteContent + '\'' +
                ", creationTime=" + creationTime +
                ", entityId=" + entityId +
                ", customerId=" + customerId +
                ", userId=" + userId +
                ", notesInInvoice=" + notesInInvoice +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerNoteWS)) return false;

        CustomerNoteWS that = (CustomerNoteWS) o;

        return noteId == that.noteId &&
                nullSafeEquals(noteTitle, that.noteTitle) &&
                nullSafeEquals(noteContent, that.noteContent) &&
                nullSafeEquals(creationTime, that.creationTime) &&
                nullSafeEquals(entityId, that.entityId) &&
                nullSafeEquals(customerId, that.customerId) &&
                nullSafeEquals(userId, that.userId) &&
                nullSafeEquals(notesInInvoice, that.notesInInvoice);
    }

    @Override
    public int hashCode() {
        int result = noteId;
        result = 31 * result + nullSafeHashCode(noteTitle);
        result = 31 * result + nullSafeHashCode(noteContent);
        result = 31 * result + nullSafeHashCode(creationTime);
        result = 31 * result + nullSafeHashCode(entityId);
        result = 31 * result + nullSafeHashCode(customerId);
        result = 31 * result + nullSafeHashCode(userId);
        result = 31 * result + nullSafeHashCode(notesInInvoice);
        return result;
    }
}
