//package com.sapienter.jbilling.server.audit.hbase.mappers;
//
//import com.sapienter.jbilling.common.SessionInternalError;
//
//
//import org.testng.annotations.Test;
//
//import javax.persistence.Column;
//
//import java.io.Serializable;
//
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.is;
//
//
///**
// * HibernateEntityMapperTest
// *
// * @author Brian Cowdery
// * @since 16-12-2012
// */
//@Test(groups = { "hbase" })
//public class HibernateEntityMapperTest {
//
//    private static class NonStringRowKey {
//    //        public int generateAuditKey(Serializable id) {
//            return 1;
//        }
//    }
//
//    @Test
//    public void testGetRowKeyNonString() {
//        HibernateEntityMapper mapper = new HibernateEntityMapper(new NonStringRowKey(), 1, AuditEvent.CREATE);
//
//        try {
//            mapper.getRowKey();
//        } catch (SessionInternalError e) {
//            assertThat(e.getMessage(), is("@HBaseKey method must return a String."));
//        }
//    }
//
//    private static class NoRowKeyMethod {
//        @Column(name = "id")
//        private Long id;
//    }
//
//    @Test
//    public void testGetRowKeyNoMethod() {
//        HibernateEntityMapper mapper = new HibernateEntityMapper(new NoRowKeyMethod(), 1, AuditEvent.CREATE);
//
//        try {
//            mapper.getRowKey();
//        } catch (SessionInternalError e) {
//            assertThat(e.getMessage(), is("Entity does not have annotated @HBaseKey method for generating row ids."));
//        }
//    }
//}
