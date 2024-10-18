package com.sapienter.jbilling.server.audit;

import com.sapienter.jbilling.server.audit.db.AuditDAO;
import com.sapienter.jbilling.server.audit.db.AuditRepository;
import com.sapienter.jbilling.server.audit.hibernate.AuditEvent;
import com.sapienter.jbilling.server.audit.mock.MockAuditableEntity;
import com.sapienter.jbilling.server.audit.mock.MockVersionedEntity;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.*;
import java.util.Date;
import static junit.framework.Assert.*;


/**
 * Created by marcolin on 27/10/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration( locations = {"/audit-test-context.xml"})
public class AuditServiceTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditRepository auditRepository;

    @Test
    public void testSaveOnAuditRepository() throws IOException {
        MockVersionedEntity entity = new MockVersionedEntity();
        entity.setId(1);
        entity.setValue("TestValue");
        AuditDAO dao = new AuditDAO();
        dao.setAuditKey("TestEntityId");
        dao.setEntity(serialize(entity));
        dao.setType(entity.getClass().getSimpleName());
        dao.setTimestamp(new Date());
        AuditDAO saved = auditRepository.save(dao);
        assertNotNull(saved);
        MockVersionedEntity versionedEntity = deserialize(saved.getEntity());
        assertEquals(entity.getId(), versionedEntity.getId());
        auditRepository.deleteAll();
    }

    @Test
    public void testPutOnAuditServiceFailIfNotAuditable() throws IOException {
        MockVersionedEntity entity = new MockVersionedEntity();
        entity.setId(1);
        auditService.put(entity, 1, AuditEvent.CREATE);
        Iterable<AuditDAO> all = auditRepository.findAll();
        assertFalse(all.iterator().hasNext());
        auditRepository.deleteAll();
    }

    @Test
    public void testPutOnAuditServiceSaveTheEntityIfAuditable() throws IOException {
        MockAuditableEntity entity = new MockAuditableEntity();
        entity.setId(1);
        auditService.put(entity, 1, AuditEvent.CREATE);
        Iterable<AuditDAO> all = auditRepository.findAll();
        assertTrue(all.iterator().hasNext());
        auditRepository.deleteAll();
    }


    public static byte[] serialize(Object obj)  {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(out);
            os.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }
    public static <T> T deserialize(byte[] data)  {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = null;
        Object readObject = null;
        try {
            is = new ObjectInputStream(in);
            readObject = is.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return (T) readObject;
    }



}
