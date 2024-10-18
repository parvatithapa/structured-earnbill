package com.sapienter.jbilling.server.audit;

import com.sapienter.jbilling.server.audit.db.AuditDAO;
import com.sapienter.jbilling.server.audit.db.AuditRepository;
import com.sapienter.jbilling.server.audit.db.DaoConverter;
import com.sapienter.jbilling.server.audit.hibernate.AuditEvent;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by marcomanzicore on 23/11/15.
 */
public class AuditServiceImpl implements AuditService, ApplicationContextAware {

    private Logger LOG = Logger.getLogger(AuditService.class);

    @Autowired
    private AuditRepository auditRepository;
    private ApplicationContext applicationContext;

    @Override

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        if (auditRepository == null)
            auditRepository = (AuditRepository) applicationContext.getBean("auditRepository");
    }

    public void setAuditRepository(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public List<Audit> find(Class type, String auditKeyPrefix) {
        String errorMessage = "There has been an issue while retrieving versioning. " +
                "Type=" + type + ", auditKeyPrefix=" + auditKeyPrefix + "]";
        return DaoConverter.convert(auditRepository.findByAuditKeyPrefix(type.getSimpleName(), auditKeyPrefix + "%"),
                errorMessage);
    }

    @Override
    public List<Audit> get(Class type, String auditKey, int versions) {
        String errorMessage = "There has been an issue while retrieving versioning. " +
                "Type=" + type + ", auditKey=" + auditKey + ", versions=" + versions + "]";
                Pageable pageable = new PageRequest(0, versions);
        return DaoConverter.convert(auditRepository.findByAuditAndAuditKey(type.getSimpleName(), auditKey, pageable),
                errorMessage);
    }

    @Override
    public Audit get(Class type, String auditKey, long timestamp) {
        String errorMessage = "There has been an issue while retrieving versioning. " +
                "Type=" + type + ", auditKey=" + auditKey + ", timestamp=" + timestamp + "]";
        return DaoConverter.convert(auditRepository.findTop25ByTypeEntityIdAndTimeStamp(type.getSimpleName(),
                auditKey, new Date(timestamp)), errorMessage);
    }

    @Override
    public void put(Object entity, Serializable id, AuditEvent event) {
        try {
            if (Auditable.class.isAssignableFrom(entity.getClass())) {
                AuditDAO dao = createAuditDao(entity, id, event);
                auditRepository.save(dao);
            }
        } catch (Exception e) {
            LOG.error("There has been an error while saving the entity version. " +
                    "[auditKey=" + ((Auditable) entity).getAuditKey(id) +
                    ", id= " + id + ", event=" + event, e);
        }
    }

    private AuditDAO createAuditDao(Object entity, Serializable id, AuditEvent event) throws Exception {
        Auditable auditable = (Auditable) entity;
        AuditDAO dao = new AuditDAO();
        dao.setAuditKey(auditable.getAuditKey(id));
        dao.setEntity(serialize(entity));
        dao.setTimestamp(new Date());
        dao.setType(entity.getClass().getSimpleName());
        dao.setEvent(event.name());
        return dao;
    }

    public byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

}