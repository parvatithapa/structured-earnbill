package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.EDIFileBL;
import com.sapienter.jbilling.server.ediTransaction.EDIFileWS;
import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemWriter;

import java.util.List;
import java.util.Map;

/**
 * Created by aman on 19/10/15.
 */
public class EDIFileItemWriter implements ItemWriter<EDIFileWS> {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EDIFileItemWriter.class));


    @Override
    public void write(List<? extends EDIFileWS> list) throws Exception {
        LOG.debug("EDI File Item Writer");
        EDIFileBL bl = new EDIFileBL();
       for (EDIFileWS ws : list) {
           try{
               LOG.debug("Saving EDI File. Id : "+ws.getId());
               bl.saveEDIFile(ws);
           }catch (Exception e){
               LOG.error("Error Occurred while saving the ediFile status");
               new SessionInternalError(e);
           }
        }
    }
}

