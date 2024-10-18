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

import com.sapienter.jbilling.server.util.db.*;

import java.util.Collection;
import java.util.Date;
import java.util.List;


public class JobExecutionBL {
    private JobExecutionHeaderDAS headerDas;
    private JobExecutionHeaderDTO header;

    public JobExecutionBL() {
        init(); 
    }

    public JobExecutionBL(int id) {
        init();
        header = headerDas.find(id);
    }

    public JobExecutionBL forExecution(long executionId) {
        header = headerDas.findByExecutionId(executionId);
        return this;
    }

    void init()  {
        headerDas = new JobExecutionHeaderDAS();
    }

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

    public JobExecutionHeaderWS[] getJobExecutionsForDateRange(Integer entityId, String jobType,Date startDate, Date endDate, int offset, int limit, String sort, String order) {
        if(sort == null || sort.isEmpty() || sort.equals("null")) {
            sort = "startDate";
        }
        if(order == null || order.isEmpty() || order.equals("null")) {
            sort = "asc";
        }
        List<JobExecutionHeaderDTO> headers = headerDas.findByTypeAndDates(entityId, jobType, startDate, endDate, offset, limit, sort, order);

        return toHeadersWs(headers);
    }

    private JobExecutionHeaderWS[] toHeadersWs(List<JobExecutionHeaderDTO> dtoHeaders) {
        JobExecutionHeaderWS[] headers = new JobExecutionHeaderWS[dtoHeaders.size()];
        int idx = 0;

        for(JobExecutionHeaderDTO dtoHeader : dtoHeaders) {
            headers[idx++] = toHeaderWs(dtoHeader);
        }

        return headers;
    }

    private JobExecutionHeaderWS toHeaderWs(JobExecutionHeaderDTO dto) {
        JobExecutionHeaderWS ws = new JobExecutionHeaderWS();
        ws.setEntityId(dto.getEntityId());
        ws.setEndDate(dto.getEndDate());
        ws.setId(dto.getId());
        ws.setJobExecutionId(dto.getJobExecutionId());
        ws.setJobType(dto.getJobType());
        ws.setStartDate(dto.getEndDate());
        ws.setStatus(dto.getStatus().name());
        ws.setLines(toLinesWs(dto.getLines()));
        return ws;
    }

    private JobExecutionLineWS[] toLinesWs(List<JobExecutionLineDTO> dtoLines) {
        JobExecutionLineWS[] lines = new JobExecutionLineWS[dtoLines.size()];
        int idx = 0;

        for(JobExecutionLineDTO dtoLine : dtoLines) {
            lines[idx++] = toLineWs(dtoLine);
        }

        return lines;
    }

    private JobExecutionLineWS toLineWs(JobExecutionLineDTO dto) {
        JobExecutionLineWS ws = new JobExecutionLineWS();
        ws.setName(dto.getName());
        ws.setType(dto.getType());
        ws.setValue(dto.getValue());
        return ws;
    }
}
