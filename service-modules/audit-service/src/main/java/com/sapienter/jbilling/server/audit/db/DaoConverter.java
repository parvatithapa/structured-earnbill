package com.sapienter.jbilling.server.audit.db;

import com.sapienter.jbilling.server.audit.Audit;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by marcomanzicore on 23/11/15.
 */
public class DaoConverter {
    private static Logger LOG = Logger.getLogger(DaoConverter.class);
    public static List<Audit> convert(List<AuditDAO> auditDAOs, final String errorMessage) {
        return auditDAOs.stream().map(dao -> DaoConverter.convert(dao, errorMessage))
                .filter(d -> d != null).collect(Collectors.toList());
    }

    public static Audit convert(AuditDAO auditDAO, String errorMessage) {
        try {
            Audit audit = new Audit();
            audit.setKey(auditDAO.getAuditKey());
            audit.setObject(auditDAO.getEntity());
            audit.setTimestamp(auditDAO.getTimestamp().getTime());
            audit.setEvent(auditDAO.getEvent());
            return audit;
        } catch (Exception e) {
            LOG.error(errorMessage, e);
        }
        return null;
    }

}
