/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.nges.export.batch.listener;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import org.apache.log4j.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


/**
 * Created by hitesh on 12/8/16.
 */
public class NGESExportSkipListener implements SkipListener<Integer, Integer>, StepExecutionListener {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NGESExportSkipListener.class));

    private String fileName;
    private BufferedWriter bw;

    @Value("#{jobParameters['errorFileName']}")
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public void onSkipInRead(Throwable throwable) {
        LOG.debug("onSkipInRead::" + throwable.getMessage());
    }

    @Override
    public void onSkipInWrite(Integer integer, Throwable throwable) {
        LOG.debug("onSkipInWrite::" + throwable.getMessage());
    }

    @Override
    public void onSkipInProcess(Integer userId, Throwable throwable) {
        write(userId + "," + throwable.getMessage());
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        prepareWriter();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        try {
            bw.close();
        } catch (IOException e) {
            LOG.debug("Exception::" + e.getLocalizedMessage());
            e.printStackTrace();
            throw new SessionInternalError("Exception::" + e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * This method used for writing the data into file.
     *
     * @param data
     * @return Nothing.
     */
    private void write(String data) {
        try {
            LOG.debug("data::" + data);
            bw.write(data);
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            LOG.debug("Exception occurred while writing data into file::" + e.getLocalizedMessage());
            e.printStackTrace();
            throw new SessionInternalError("Exception occurred while writing data into file::" + e.getLocalizedMessage());
        }
    }

    /**
     * This method used for creating the BufferedWriter Object.
     *
     * @return Nothing.
     */
    private void prepareWriter() {
        try {
            bw = new BufferedWriter(new FileWriter(getFileName(), true));
        } catch (IOException e) {
            LOG.debug("Exception occurred while preparing the writer::" + e.getLocalizedMessage());
            e.printStackTrace();
            throw new SessionInternalError("Exception occurred while preparing the writer::" + e.getLocalizedMessage());
        }
    }
}
