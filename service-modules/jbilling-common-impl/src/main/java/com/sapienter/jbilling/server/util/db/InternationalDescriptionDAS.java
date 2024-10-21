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
package com.sapienter.jbilling.server.util.db;

import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import org.hibernate.Query;
import org.springframework.util.StringUtils;

import java.util.Collection;


/**
 * @author abimael
 */
public class InternationalDescriptionDAS extends AbstractDAS<InternationalDescriptionDTO> {

    private JbillingTableDAS jtDAS; // injected by Spring

    // should only be created from Spring
    protected InternationalDescriptionDAS() {
        super();
    }

    public void setJbDAS(JbillingTableDAS util) {
        this.jtDAS = util;
    }

    public InternationalDescriptionDTO findIt(String table,
                                              Integer foreignId, String column, Integer language) {

        if (foreignId == null || foreignId == 0) {
            return null;
        }

        InternationalDescriptionId idi =
                new InternationalDescriptionId(jtDAS.findByName(table).getId(),
                        (foreignId == null) ? 0 : foreignId, column, (language == null) ? 0 : language);

        return find(idi); // this should cache ok
    }

    public Collection<InternationalDescriptionDTO> exists(String table, String column, String content, Integer language) {

        jtDAS = (JbillingTableDAS) Context.getBean(Context.Name.JBILLING_TABLE_DAS);

        final String QUERY = "SELECT a " +
                "FROM InternationalDescriptionDTO a " +
                "WHERE a.id.tableId = :tableId " +
                "AND a.id.psudoColumn = :psudoColumn " +
                "AND a.id.languageId = :languageId " +
                "AND UPPER(a.content) = UPPER(:content)";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("tableId", jtDAS.findByName(table).getId());
        query.setParameter("psudoColumn", column);
        query.setParameter("languageId", language);
        query.setParameter("content", content);
        return query.list();
    }

    public Collection<InternationalDescriptionDTO> roleExists(String table, String column, String content, Integer language, Integer companyId) {
        jtDAS = (JbillingTableDAS) Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        final String ROLEIDS_QUERY = "SELECT a.id " +
                "FROM RoleDTO a " +
                "WHERE a.company.id = :companyId";
        Query roleIdsQuery = getSession().createQuery(ROLEIDS_QUERY);
        roleIdsQuery.setParameter("companyId", companyId);

        final String QUERY = "SELECT a " +
                "FROM InternationalDescriptionDTO a " +
                "WHERE a.id.tableId = :tableId " +
                "AND a.id.psudoColumn = :psudoColumn " +
                "AND a.id.languageId = :languageId " +
                "AND UPPER(a.content) = UPPER(:content)" +
                "AND a.id.foreignId in (:foreignIds)";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("tableId", jtDAS.findByName(table).getId());
        query.setParameter("psudoColumn", column);
        query.setParameter("languageId", language);
        query.setParameter("content", content);
        query.setParameterList("foreignIds", roleIdsQuery.list());

        return query.list();
    }

    public InternationalDescriptionDTO create(String table, Integer foreignId, String column,
                                              Integer language, String message) {

        InternationalDescriptionId idi = new InternationalDescriptionId(
                jtDAS.findByName(table).getId(), foreignId, column, language);

        InternationalDescriptionDTO inter = new InternationalDescriptionDTO();
        inter.setId(idi);
        inter.setContent(message);

        return save(inter);

    }

    public Collection<InternationalDescriptionDTO> findByTable_Row(String table, Integer foreignId) {
        final String QUERY = "SELECT a " +
                "FROM InternationalDescriptionDTO a, JbillingTable b " +
                "WHERE a.id.tableId = b.id " +
                "AND b.name = :table " +
                "AND a.id.foreignId = :foreing ";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("table", table);
        query.setParameter("foreing", foreignId);
        return query.list();
    }


    public Collection<InternationalDescriptionDTO> findAll(int tableId, int foreignId, String psudoColumn) {
        final String QUERY = "SELECT a " + "FROM InternationalDescriptionDTO a " + "WHERE a.id.tableId = :tableId "
                + "AND a.id.foreignId = :foreignId " + "AND a.id.psudoColumn = :psudoColumn ";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("tableId", tableId);
        query.setParameter("foreignId", foreignId);
        query.setParameter("psudoColumn", psudoColumn);
        return query.list();
    }


    public void delete(int tableId, int foreignId, String psudoColumn, int languageId) {
        final String QUERY = "DELETE " + "InternationalDescriptionDTO a " + "WHERE a.id.tableId = :tableId "
                + "AND a.id.foreignId = :foreignId " + "AND a.id.psudoColumn = :psudoColumn "
                + "AND a.id.languageId = :languageId ";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("tableId", tableId);
        query.setParameter("foreignId", foreignId);
        query.setParameter("psudoColumn", psudoColumn);
        query.setParameter("languageId", languageId);
        query.executeUpdate();
    }

    public static InternationalDescriptionDAS getInstance() {
        return new InternationalDescriptionDAS();
    }

    public Collection<InternationalDescriptionDTO> findOrderPeriodByDescription(String descrption) {

        final String QUERY = "SELECT a " +
                "FROM InternationalDescriptionDTO a, JbillingTable b " +
                "WHERE  b.name = :table " +
                "AND a.content = :description ";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("table", Constants.TABLE_ORDER_PERIOD);
        query.setParameter("description", descrption);
        return query.list();
    }

    public InternationalDescriptionDTO findAssetStatusByForeignIdOrContent(Integer foreignId, String content) {
        String QUERY = "select intDes from InternationalDescriptionDTO intDes WHERE table_id = 108 ";

        if (foreignId != null && foreignId > 0) {
            QUERY = QUERY + " AND intDes.id.foreignId= " + foreignId;
        }

        if (StringUtils.hasLength(content)) {
            QUERY = QUERY + String.format(" AND intDes.content = '%s'" , content);
        }
        Query query = getSession().createQuery(QUERY);
        return (InternationalDescriptionDTO) query.uniqueResult();
    }
}
