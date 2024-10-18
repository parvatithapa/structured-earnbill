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

package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.server.util.db.JobExecutionHeaderDAS;
import com.sapienter.jbilling.server.util.db.JobExecutionHeaderDTO;
import com.sapienter.jbilling.server.util.db.JobExecutionLineDTO;

import java.util.Date;

/**
 * Collect statistics around a job execution
 */
public class JobExecutionService {
    private JobExecutionHeaderDAS headerDas;
    private JobExecutionHeaderDTO header;

    public JobExecutionService() {
        init();
    }

    public JobExecutionService(int id) {
        init();
        header = headerDas.find(id);
    }

    public JobExecutionService forExecution(long executionId) {
        header = headerDas.findByExecutionId(executionId);
        return this;
    }

    void init()  {
        headerDas = new JobExecutionHeaderDAS();
    }

    /**
     * Create a header when a job gets started.
     *
     * @param jobExecutionId ID from Spring Batch
     * @param type Value to differentiate job types
     * @param startDate Job start date
     * @param entityId
     * @return
     */
    public JobExecutionHeaderDTO startJob(long jobExecutionId, String type, Date startDate, int entityId) {
        JobExecutionHeaderDTO header = new JobExecutionHeaderDTO();
        header.setStatus(JobExecutionHeaderDTO.Status.STARTED);
        header.setJobExecutionId(jobExecutionId);
        header.setJobType(type);
        header.setStartDate(startDate);
        header.setEntityId(entityId);
        headerDas.save(header);

        return header;
    }

    public void endJob(Date endDate) {
        endJob(endDate, JobExecutionHeaderDTO.Status.SUCCESS);
    }

    public void endJob(Date endDate, JobExecutionHeaderDTO.Status status) {
        header.setEndDate(endDate);
        header.setStatus(status);
    }

    public void addLine(String type, String name, String value) {
        JobExecutionLineDTO line = new JobExecutionLineDTO();
        line.setType(type);
        line.setName(name);
        line.setValue(value);
        line.setHeader(header);
        header.getLines().add(line);
    }

    public void updateLine(String type, String name, String value) {
        for(JobExecutionLineDTO line : header.getLines()) {
            if(line.getType().equals(type) && line.getName().equals(name)) {
                line.setValue(value);
                return;
            }
        }

        JobExecutionLineDTO line = new JobExecutionLineDTO();
        line.setType(type);
        line.setName(name);
        line.setValue(value);
        line.setHeader(header);
        header.getLines().add(line);
    }

    public void incrementLine(String type, String name) {
        for(JobExecutionLineDTO line : header.getLines()) {
            if(line.getType().equals(type) && line.getName().equals(name)) {
                line.setValue(Integer.toString(Integer.parseInt(line.getValue())+1));
                return;
            }
        }

        JobExecutionLineDTO line = new JobExecutionLineDTO();
        line.setType(type);
        line.setName(name);
        line.setValue("1");
        line.setHeader(header);
        header.getLines().add(line);
    }
}
