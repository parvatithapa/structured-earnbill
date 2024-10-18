package com.sapienter.jbilling.server.ediTransaction.db;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.EDIStatisticWS;
import com.sapienter.jbilling.server.ediTransaction.TransactionType;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.*;
import org.hibernate.sql.JoinType;
import org.hibernate.type.StandardBasicTypes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EDIFileDAS extends AbstractDAS<EDIFileDTO> {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EDIFileDAS.class));
    public int isErrorFieldExist(Integer fileId) {
        Criteria criteria = getSession().createCriteria(EDIFileDTO.class)
                .createAlias("ediFileRecords", "records")
                .createAlias("records.fileFields", "fields")
                .add(Restrictions.and(Restrictions.ne("fields.comment", ""), Restrictions.isNotNull("fields.comment")))
                .setMaxResults(1);

        return ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).intValue();
    }

    public List<EDIFileDTO> getEDIFilesUsingStatus(Integer entityId, Integer ediTypeId, String statusName) {
        Criteria criteria = getSession().createCriteria(EDIFileDTO.class)
                .createAlias("entity", "entity")
                .createAlias("ediType", "type")
                .createAlias("fileStatus", "status")
                .add(Restrictions.eq("type.id", ediTypeId))
                .add(Restrictions.like("status.name", statusName));
        if(entityId!=null){
            criteria.add(Restrictions.eq("entity.id", entityId));
        }
        List<EDIFileDTO> ediFileDTOs = (List<EDIFileDTO>) criteria.list();
        return ediFileDTOs;
    }

    public List<EDIFileDTO> getEDIFilesUsingStatus(Integer entityId,  Integer statusId) {
        Criteria criteria = getSession().createCriteria(EDIFileDTO.class)
                .createAlias("entity", "entity")
                .createAlias("fileStatus", "status")
                .add(Restrictions.eq("status.id", statusId));
        if(entityId!=null){
            criteria.add(Restrictions.eq("entity.id", entityId));
        }
        List<EDIFileDTO> ediFileDTOs = (List<EDIFileDTO>) criteria.list();
        return ediFileDTOs;
    }

    public EDIFileDTO getEDIFileForEnrollment(String enrollmentId, Integer entityId, Integer ediTypeId) {
        EDIFileDTO fileDTO = null;
        Criteria criteria = getSession().createCriteria(EDIFileDTO.class)
                .createAlias("entity", "entity")
                .createAlias("ediType", "ediType")
                .createAlias("ediFileRecords", "fileRecords")
                .createAlias("ediFileRecords.fileFields", "fileFields")
                .add(Restrictions.eq("type", TransactionType.OUTBOUND))
                .add(Restrictions.eq("entity.id", entityId))
                .add(Restrictions.eq("ediType.id", ediTypeId))
                .add(Restrictions.eq("fileFields.ediFileFieldKey", FileConstants.TRANS_REF_NR))
                .add(Restrictions.ilike("fileFields.ediFileFieldValue", "%" + enrollmentId, MatchMode.END))
                .addOrder(Order.desc("createDatetime"));

        List<EDIFileDTO> ediFileDTOs = (List<EDIFileDTO>) criteria.list();
        if (ediFileDTOs.size() > 0) {
            fileDTO = ediFileDTOs.get(0);
        }
        return fileDTO;
    }

    public List<EDIStatisticWS> getEdiStatistics(Integer entityId, Date startDate, Date endDate, Integer ediTypeId) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        sb.append(" et.name as transactionType, ");
        sb.append("	sum(case when ef.transaction_type = 'INBOUND' then 1 else 0 end) as inbound, ");
        sb.append("	sum(case when ef.transaction_type = 'OUTBOUND' then 1 else 0 end) as outbound ");
        sb.append("from ");
        sb.append("	edi_file ef ");
        sb.append("join ");
        sb.append("	edi_type et on (et.id = ef.edi_type_id) ");
        sb.append("where ef.entity_id = :entityId and (ef.create_datetime between :startDate and :endDate) and (:ediTypeId is null or ef.edi_type_id = :ediTypeId) ");
        sb.append("group by et.name ");
        sb.append("order by et.name");

        Query query = getSessionFactory().getCurrentSession().createSQLQuery(sb.toString())
                .addScalar("transactionType", StandardBasicTypes.STRING)
                .addScalar("inbound", StandardBasicTypes.LONG)
                .addScalar("outbound", StandardBasicTypes.LONG)
                .setParameter("entityId", entityId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("ediTypeId", ediTypeId, StandardBasicTypes.INTEGER);

        List<Object[]> rows = query.list();
        List<EDIStatisticWS> statistics = new ArrayList<EDIStatisticWS>();
        for (Object[] row : rows) {
            EDIStatisticWS statistic = new EDIStatisticWS();
            statistic.setTransactionType((String) row[0]);
            statistic.setInbound((Long) row[1]);
            statistic.setOutbound((Long) row[2]);
            statistics.add(statistic);
        }

        return statistics;
    }

    public List<EDIStatisticWS> getEdiStatisticsWithExceptions(Integer entityId, Date startDate, Date endDate, Integer ediTypeId) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        sb.append(" et.name as transactionType, ");
        sb.append("	sum(case when ef.transaction_type = 'INBOUND' then 1 else 0 end) as inbound, ");
        sb.append("	sum(case when ef.transaction_type = 'OUTBOUND' then 1 else 0 end) as outbound ");
        sb.append("from ");
        sb.append("	edi_file ef ");
        sb.append("join ");
        sb.append("	edi_type et on (et.id = ef.edi_type_id) ");
        sb.append("join ");
        sb.append("	edi_file_status efs on (efs.id = ef.edi_status_id) ");
        sb.append("where ef.entity_id =:entityId and efs.is_error and (ef.create_datetime between :startDate and :endDate) and (:ediTypeId is null or ef.edi_type_id = :ediTypeId) ");
        sb.append("group by et.name ");
        sb.append("order by et.name");

        Query query = getSessionFactory().getCurrentSession().createSQLQuery(sb.toString())
                .addScalar("transactionType", StandardBasicTypes.STRING)
                .addScalar("inbound", StandardBasicTypes.LONG)
                .addScalar("outbound", StandardBasicTypes.LONG)
                .setParameter("entityId", entityId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("ediTypeId", ediTypeId, StandardBasicTypes.INTEGER);

        List<Object[]> rows = query.list();
        List<EDIStatisticWS> statistics = new ArrayList<EDIStatisticWS>();
        for(Object[] row : rows){
            EDIStatisticWS statistic = new EDIStatisticWS();
            statistic.setTransactionType((String) row[0]);
            statistic.setInbound((Long) row[1]);
            statistic.setOutbound((Long) row[2]);
            statistics.add(statistic);
        }

        return statistics;
    }

    public List<EDIFileDTO> getEDIFilesWithExceptions(Integer entityId, Integer ediTypeId, Integer max, Integer offset) {
        Criteria criteria = getSession().createCriteria(EDIFileDTO.class)
                .createAlias("fileStatus", "status").add(Restrictions.eq("status.error", true));

        if (entityId != null) {
            criteria.createAlias("entity", "entity").add(Restrictions.eq("entity.id", entityId));
        }

        if (ediTypeId != null) {
            criteria.createAlias("ediType", "ediType").add(Restrictions.eq("ediType.id", ediTypeId));
        }

        if (max != null) {
            criteria.setMaxResults(max);
        }

        if (offset != null) {
            criteria.setFirstResult(offset);
        }

        return criteria.list();
    }

    public Long getEDIFilesWithExceptionsCount(Integer entityId, Integer ediTypeId) {
        Criteria criteria = getSession().createCriteria(EDIFileDTO.class)
                .createAlias("fileStatus", "status").add(Restrictions.eq("status.error", true));

        if (entityId != null) {
            criteria.createAlias("entity", "entity").add(Restrictions.eq("entity.id", entityId));
        }

        if (ediTypeId != null) {
            criteria.createAlias("ediType", "ediType").add(Restrictions.eq("ediType.id", ediTypeId));
        }

        criteria.setProjection(Projections.rowCount());

        return (Long) criteria.uniqueResult();
    }

    public boolean isRecordExistForFileFieldKeyAndValue(Integer entityId, Integer ediTypeId, Integer ediFileId, String key, String value, TransactionType transactionType){
        if(value == null) {
            return false;
        }

        Criteria criteria = getSession().createCriteria(EDIFileDTO.class)
                .createAlias("entity", "entity")
                .createAlias("ediType", "type")
                .createAlias("fileStatus", "status")
                .createAlias("ediFileRecords", "ediFileRecord")
                .createAlias("ediFileRecord.fileFields", "fileField")
                .add(Restrictions.eq("type", transactionType))
                .add(Restrictions.eq("entity.id", entityId))
                .add(Restrictions.eq("type.id", ediTypeId))
                .add(Restrictions.eq("fileField.ediFileFieldKey", key))
                .add(Restrictions.eq("fileField.ediFileFieldValue", value))
                .add(Restrictions.not(Restrictions.eq("id", ediFileId)))
                .setProjection(Projections.rowCount());
        Long count = (Long) criteria.uniqueResult();
        return  ((Long) criteria.uniqueResult() == 0);

    }

    public List<Integer> findFileByData(Conjunction conjunction) {
        Criteria criteria = getSession().createCriteria(EDIFileDTO.class)
                .createAlias("entity", "entity")
                .createAlias("ediType", "ediType")
                .createAlias("ediFileRecords", "fileRecords")
                .createAlias("fileStatus", "status")
                .createAlias("exceptionCode", "code", JoinType.LEFT_OUTER_JOIN)
                .createAlias("ediFileRecords.fileFields", "fileFields")
                .setProjection(Projections.property("id"));
        criteria.add(conjunction);
        return(List<Integer>) criteria.list();
    }

    public List<EDIFileDTO> findEDIFiles(Conjunction conjunction) {
        Criteria criteria = getSession().createCriteria(EDIFileDTO.class)
                .createAlias("entity", "entity")
                .createAlias("ediType", "ediType")
                .createAlias("ediFileRecords", "fileRecords")
                .createAlias("fileStatus", "status")
                .createAlias("exceptionCode", "code", JoinType.LEFT_OUTER_JOIN)
                .createAlias("ediFileRecords.fileFields", "fileFields");
        criteria.add(conjunction);
        return  criteria.list();
    }

    public List<Object[]> findDataFromField(Conjunction conjunction) {
        Criteria criteria = getSession().createCriteria(EDIFileFieldDTO.class)
                .createAlias("ediFileRecord", "record")
                .createAlias("record.ediFile", "file");
        criteria.add(conjunction);

        ProjectionList projList = Projections.projectionList();
        projList.add(Projections.property("file.id"));
        projList.add(Projections.property("ediFileFieldKey"));
        projList.add(Projections.property("ediFileFieldValue"));
        criteria.setProjection(projList);
        criteria.addOrder(Order.desc("id"));
        return criteria.list();
    }

    public EDIFileDTO findEDIFile(Conjunction conjunction) {
        Criteria criteria = getSession().createCriteria(EDIFileDTO.class)
                .createAlias("entity", "entity")
                .createAlias("ediType", "ediType")
                .createAlias("ediFileRecords", "fileRecords")
                .createAlias("fileStatus", "status")
                .createAlias("ediFileRecords.fileFields", "fileFields");
        criteria.add(conjunction);
        criteria.addOrder(Order.desc("createDatetime"));
        criteria.setMaxResults(1);
        return (EDIFileDTO)criteria.uniqueResult();
    }

    public ScrollableResults findAllByEDIType(int ediTypeId) {
        return getSession().createCriteria(EDIFileDTO.class)
                .createAlias("ediType", "ediType")
                .add(Restrictions.eq("ediType.id", ediTypeId))
                .scroll();
    }

    public void deleteAllFilesByEDIType(int ediTypeId, int companyId) {
        //Check that edi type exist in company
        EDITypeDTO type = new EDITypeDAS().findByIdAndCompanyId(ediTypeId, companyId);
        if (type == null) {
            throw new SessionInternalError("EDI type does not exist : " + ediTypeId);
        }

        // fetch all files belong to this edi type.
        ScrollableResults files = findAllByEDIType(ediTypeId);

        try {
            while (files.next()) {
                EDIFileDTO file = (EDIFileDTO) files.get()[0];
                LOG.debug("Deleting EDI file : %d ", file.getId());
                delete((EDIFileDTO) files.get()[0]);
            }
        }finally {
            files.close();
        }
    }

}