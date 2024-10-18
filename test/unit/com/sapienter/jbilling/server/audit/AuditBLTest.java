package com.sapienter.jbilling.server.audit;

import com.sapienter.jbilling.test.TestUtils;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import javax.persistence.Column;
import javax.persistence.Table;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


/**
 * AuditBLTest
 *
 * @author Brian Cowdery
 * @since 18-12-2012
 */
@Test(groups = { "audit" }, testName = "AuditBLTest")
public class AuditBLTest  {

    private AuditBL auditBL = new AuditBL();

    /**
     * Test class for converting to and from audit versions
     */
    @Table(name = "audit_bean")
    public static class AuditableBean implements Serializable {

        private Long id;
        private String name;
        private BigDecimal decimal;
        private Date created;
        private Boolean wrapped;
        private boolean primitive;
        private int optlock;

        @Column(name = "id")
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Column(name = "name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Column(name = "decimal_number")
        public BigDecimal getDecimal() {
            return decimal;
        }

        public void setDecimal(BigDecimal decimal) {
            this.decimal = decimal;
        }

        @Column(name = "created_date")
        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        @Column(name = "is_wrapped")
        public Boolean getWrapped() {
            return wrapped;
        }

        public void setWrapped(Boolean wrapped) {
            this.wrapped = wrapped;
        }

        @Column(name = "is_primitive")
        public boolean getPrimitive() {
            return primitive;
        }

        public void setPrimitive(boolean primitive) {
            this.primitive = primitive;
        }

        public boolean isPrimitive() {
            return primitive;
        }

        @Column(name = "optlock")
        public int getOptlock() {
            return optlock;
        }

        public void setOptlock(int optlock) {
            this.optlock = optlock;
        }
    }


    @Test
    public void testExtractColumnValues() {
        Date date = TestUtils.AsDate(2012, 1, 1);

        AuditableBean bean = new AuditableBean();
        bean.setId(1L);
        bean.setName("test bean");
        bean.setDecimal(new BigDecimal("1.00"));
        bean.setCreated(date);
        bean.setWrapped(true);
        bean.setPrimitive(false);
        bean.setOptlock(2);

        Map<String, String> columnValues = auditBL.getColumnValues(bean);

        // found all annotated fields
        Set<String> columnNames = columnValues.keySet();
        assertThat(columnNames, Matchers.<String>hasItem("id"));
        assertThat(columnNames, Matchers.<String>hasItem("name"));
        assertThat(columnNames, Matchers.<String>hasItem("decimal_number"));
        assertThat(columnNames, Matchers.<String>hasItem("created_date"));
        assertThat(columnNames, Matchers.<String>hasItem("is_wrapped"));
        assertThat(columnNames, Matchers.<String>hasItem("is_primitive")); // @Column on getPrimitive() not isPrimitive()!!
        assertThat(columnNames, Matchers.<String>hasItem("optlock"));

        assertThat(columnValues.size(), is(7));

        // correct values for set fields
        assertThat(columnValues.get("id"), is("1"));
        assertThat(columnValues.get("name"), is("test bean"));
        assertThat(columnValues.get("decimal_number"), is("1.00"));
        assertThat(columnValues.get("created_date"), Matchers.startsWith("2012-01-01 00:00:00"));
        assertThat(columnValues.get("is_wrapped"), is("true"));
        assertThat(columnValues.get("is_primitive"), is("false"));
        assertThat(columnValues.get("optlock"), is("2"));
    }

    @Test
    public void testRestore() throws Exception {
        Date date = TestUtils.AsDate(2012, 1, 1);

        Audit version = new Audit();
        AuditableBean bean = new AuditableBean();
        bean.setId(1L);
        bean.setName("test bean");
        bean.setDecimal(new BigDecimal("1.23"));
        bean.setCreated(date);
        bean.setWrapped(false);
        bean.setPrimitive(true);
        bean.setOptlock(2);

        version.setObject(serialize(bean));
        auditBL.restore(AuditableBean.class, version, bean);

        assertThat(bean.getId(), is(1L));
        assertThat(bean.getName(), is("test bean"));
        assertThat(bean.getDecimal(), is(new BigDecimal("1.23")));
        assertThat(bean.getCreated(), is(date));
        assertThat(bean.getWrapped(), is(false));
        assertThat(bean.isPrimitive(), is(true));
        assertThat(bean.getOptlock(), is(2));
    }

    @Test
    public void testNullValues() throws Exception {
        // hbase will return empty strings for null values
        // test restoring a map of values that only contains empty strings
        Audit version = new Audit();
        AuditableBean bean = new AuditableBean();
        bean.setId(null);
        bean.setName(null);
        bean.setDecimal(null);
        bean.setCreated(null);
        bean.setWrapped(null);
        bean.setPrimitive(false);
        bean.setOptlock(0);

        version.setObject(serialize(bean));
        auditBL.restore(AuditableBean.class, version, bean);

        assertThat(bean.getId(), Matchers.nullValue());
        assertThat(bean.getName(), Matchers.nullValue());
        assertThat(bean.getDecimal(), Matchers.nullValue());
        assertThat(bean.getCreated(), Matchers.nullValue());
        assertThat(bean.getWrapped(), Matchers.nullValue());
        assertThat(bean.isPrimitive(), is(false));  // primitive bool default false
        assertThat(bean.getOptlock(), is(0));       // primitive int default 0
    }

    @Test
    public void testRestoreDate() throws Exception {
        Date date = TestUtils.AsDate(2012, 1, 1);

        Audit version = new Audit();
        AuditableBean bean = new AuditableBean();
        bean.setCreated(date);
        version.setObject(serialize(bean));
        auditBL.restore(AuditableBean.class, version, bean);

        assertThat(bean.getCreated(), is(date));
    }

    @Test
    public void testRestoreWrappedBoolean() throws Exception {
        Audit version = new Audit();
        AuditableBean bean = new AuditableBean();
        bean.setWrapped(true);
        version.setObject(serialize(bean));
        auditBL.restore(AuditableBean.class, version, bean);
        assertThat(bean.getWrapped(), is(true));
    }

    @Test
    public void testRestorePrimitiveBoolean() throws Exception {
        Audit version = new Audit();

        AuditableBean bean = new AuditableBean();
        bean.setPrimitive(true);
        version.setObject(serialize(bean));
        auditBL.restore(AuditableBean.class, version, bean);
        assertThat(bean.getPrimitive(), is(true));
    }


    public byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

}
