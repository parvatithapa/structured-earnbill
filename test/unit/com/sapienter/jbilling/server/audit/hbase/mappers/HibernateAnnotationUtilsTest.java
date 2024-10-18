//package com.sapienter.jbilling.server.audit.hbase.mappers;
//
//
//import com.sapienter.jbilling.server.audit.hibernate.HibernateAnnotationUtils;
//import com.sapienter.jbilling.server.util.hbase.model.HBaseEntityModel;
//import org.testng.annotations.Test;
//
//import javax.persistence.Column;
//import javax.persistence.Table;
//
//import java.io.Serializable;
//
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.*;
//
///**
// * HibernateAnnotationUtilsTest
// *
// * @author Brian Cowdery
// * @since 16-12-2012
// */
//@Test(groups = { "hbase" })
//public class HibernateAnnotationUtilsTest {
//
//    @Table(name = "test")
//    private static class TestModel {
//
//        @Column(name = "id_col")
//        private Long id;
//
//        @Column(name = "name_col")
//        public String name;
//
//        public String description;
//
//        public String notAColumn;
//
//        @Column(name = "description_col")
//        public String getDescription() {
//            return description;
//        }
//
//        public void setDescription(String description) {
//            this.description = description;
//        }
//
//        public String getNotAColumn() {
//            return notAColumn;
//        }
//
//        public void setNotAColumn(String notAColumn) {
//            this.notAColumn = notAColumn;
//        }
//
//    //        public String generateAuditKey(Serializable id) {
//            return id + "-key";
//        }
//    }
//
//
//    @Test
//    public void testBuildEntityModel() {
//        HBaseEntityModel model = HibernateAnnotationUtils.buildEntityModel(TestModel.class);
//
//        assertThat(model.getTableName(), is("test"));
//
//        assertThat(model.getColumnFields().size(), is(3));
//        assertThat(model.getColumnFields().get("id"), is("id_col"));
//        assertThat(model.getColumnFields().get("name"), is("name_col"));
//        assertThat(model.getColumnFields().get("description"), is("description_col"));
//
//        assertThat(model.getKeyMethod(), is(not(nullValue())));
//    }
//
//    private static class TestNonJavaBeans {
//
//        private Long id;
//        private String name;
//
//        @Column(name = "id")
//        public Long getIdentifier() {
//            return id;
//        }
//
//        public void setIdentifier(Long id) {
//            this.id = id;
//        }
//
//        @Column(name = "name")
//        public String getNameField() {
//            return name;
//        }
//
//        public void setNameField(String name) {
//            this.name = name;
//        }
//    }
//
//    @Test
//    public void testBadFieldNames() {
//        // should not trip up annotation processing
//        HBaseEntityModel model = HibernateAnnotationUtils.buildEntityModel(TestNonJavaBeans.class);
//
//        // but the field names will be wrong since the method names don't follow the JavaBeans spec
//        assertThat(model.getColumnFields().size(), is(2));
//        assertThat(model.getColumnFields().get("identifier"), is("id"));
//        assertThat(model.getColumnFields().get("nameField"), is("name"));
//    }
//
//    @Test
//    public void testMissingTableName() {
//        // generated table name will be null
//        HBaseEntityModel model = HibernateAnnotationUtils.buildEntityModel(TestNonJavaBeans.class);
//
//        assertThat(model.getTableName(), is(nullValue()));
//    }
//
//
//    @Test
//    public void testGetHibernateEntityModelFromCache() {
//        HBaseEntityModel one = HibernateAnnotationUtils.getHibernateEntityModel(TestModel.class);
//        HBaseEntityModel two = HibernateAnnotationUtils.getHibernateEntityModel(TestModel.class);
//
//        assertThat(one, sameInstance(two));
//    }
//}
