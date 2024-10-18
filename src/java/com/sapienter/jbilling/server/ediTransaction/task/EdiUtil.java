package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.ediTransaction.EDIFileFieldWS;
import com.sapienter.jbilling.server.ediTransaction.EDIFileRecordWS;
import com.sapienter.jbilling.server.ediTransaction.EDIFileWS;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.user.UserWS;
import org.apache.log4j.Logger;

import java.util.Arrays;

/**
 * Commons methods used in the EDI infrastructure.
 *
 * @author Gerhard Maree
 * @since 09-11-2015
 */
public class EdiUtil {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EdiUtil.class));

    /**
     * Check if the user has already been terminated.
     *
     * @param userWS
     * @return true if user is terminated/dropped
     */
    public static boolean userAlreadyTerminatedOrDropped(UserWS userWS) {
        if (userWS != null) {
            for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
                if (metaFieldValueWS.getFieldName().equals(FileConstants.TERMINATION_META_FIELD)) {
                    LOG.debug("Termination meta field exist.");
                    if (metaFieldValueWS.getValue().equals(FileConstants.TERMINATION_PROCESSING) || metaFieldValueWS.getValue().equals(FileConstants.DROPPED)) {
                        LOG.error("Customer" + userWS.getUserName() + " is either in process or dropped.");

                        return true;
                    }
                }
            }
        }
        return false;
    }

    /*
    * Finding record value on the basis of recordKey, fieldKey
    *
    * @params ediFile
    * @params recordKey ediFile record key (for example KEY, HDR etc)
    * @params fieldKey edi record field key
    *
    * @return value of the field key
    * */
    public static String findRecord(EDIFileWS ediFile, String recordKey, String fieldKey){
        EDIFileRecordWS ediFileRecordWS= Arrays.asList(ediFile.getEDIFileRecordWSes()).stream().filter((EDIFileRecordWS ediFileRecord) -> ediFileRecord.getHeader().equals(recordKey)).findFirst().get();
        if(ediFileRecordWS==null){
            return null;
        }

        EDIFileFieldWS ediFileFieldWS=Arrays.asList(ediFileRecordWS.getEdiFileFieldWSes()).stream().filter((EDIFileFieldWS ediFileField) -> ediFileField.getKey().equals(fieldKey)).findFirst().get();
        if(ediFileFieldWS==null){
            return null;
        }
        return ediFileFieldWS.getValue();

    }
}
