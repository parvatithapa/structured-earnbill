package com.sapienter.jbilling.server.mediation.process.db;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * Created by ashwin on 21/05/18.
 */
@Entity
@TableGenerator(
		name            = "mediation_process_cdr_count_GEN",
		table           = "jbilling_seqs",
		pkColumnName    = "name",
		valueColumnName = "next_id",
		pkColumnValue   = "mediation_process_cdr_count")
@Table(name = "mediation_process_cdr_count")
public class MediationProcessCDRCountDAO {

    private Integer id;
    private UUID processId;
    private String callType;
    private Integer count;
    private String recordStatus;

    public MediationProcessCDRCountDAO() {

    }

    private MediationProcessCDRCountDAO(UUID processId, String callType, Integer count, String recordStatus) {
        this.processId = processId;
        this.callType = callType;
        this.count = count;
        this.recordStatus = recordStatus;
    }

    public static MediationProcessCDRCountDAO of(UUID processId, String callType, Integer count, String recordStatus) {
        return new MediationProcessCDRCountDAO(processId, callType, count, recordStatus);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "mediation_process_cdr_count_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name="process_id", nullable = false)
    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    @Column(name="call_type", nullable = false)
    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    @Column(name="count")
    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @Column(name="record_status", nullable = false)
    public String getRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(String recordStatus) {
        this.recordStatus = recordStatus;
    }

    @Override
    public String toString() {
        return String
                .format("MediationProcessCDRCountDAO [id=%s, processId=%s, callType=%s, count=%s, recordStatus=%s]",
                        id, processId, callType, count, recordStatus);
    }


}