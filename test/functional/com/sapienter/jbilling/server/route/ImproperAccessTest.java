package com.sapienter.jbilling.server.route;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pricing.RouteRecordWS;
import com.sapienter.jbilling.server.user.MatchingFieldWS;
import com.sapienter.jbilling.server.user.RouteRateCardWS;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/12/15.
 */
@Test(testName = "route.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final int PRANCING_PONY_ID = 1;
    private static final int PRANCING_PONY_ROUTE_ID = 2;
    private static final int PRANCING_PONY_ROUTE_RATECARD_ID = 1;
    private static final int PRANCING_PONY_MATCHING_FIELD_ID = 21;
    private static final int MORDOR_DATA_TABLE_QUERY_ID_ID = 1;
    private static final int PRANCING_PONY_ENTITY_ID = 1;
    private static final int MORDOR_ENTITY_ID = 2;

    @Test
    public void testDeleteRoute() {
        // Cross Company
        try {
            capsuleAdminApi.deleteRoute(PRANCING_PONY_ROUTE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetRoute() {
        // Cross Company
        try {
            capsuleAdminApi.getRoute(PRANCING_PONY_ROUTE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteMatchingField() {
        // Cross Company
        try {
            capsuleAdminApi.deleteMatchingField(PRANCING_PONY_MATCHING_FIELD_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_MATCHING_FIELD_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetMatchingField() {
        // Cross Company
        try {
            capsuleAdminApi.getMatchingField(PRANCING_PONY_MATCHING_FIELD_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_MATCHING_FIELD_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdateMatchingField() {
        MatchingFieldWS matchingFieldWS = oscorpAdminApi.getMatchingField(PRANCING_PONY_MATCHING_FIELD_ID);
        // Cross Company
        try {
            capsuleAdminApi.updateMatchingField(matchingFieldWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_MATCHING_FIELD_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteRouteRateCard() {
        // Cross Company
        try {
            capsuleAdminApi.deleteRouteRateCard(PRANCING_PONY_ROUTE_RATECARD_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdateRouteRateCard() {
        RouteRateCardWS routeRateCardWS = oscorpAdminApi.getRouteRateCard(PRANCING_PONY_ROUTE_RATECARD_ID);

        File file = null;
        try {
            file = File.createTempFile("testUpdateRouteRateCard" + new Double(Math.random() * 1000).intValue(), "tmp");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Cross Company
        try {
            capsuleAdminApi.updateRouteRateCard(routeRateCardWS, file);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetRouteRateCard() {
        // Cross Company
        try {
            capsuleAdminApi.getRouteRateCard(PRANCING_PONY_ROUTE_RATECARD_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdateRouteRecord() {
        RouteRecordWS routeRecordWS = new RouteRecordWS();
        routeRecordWS.setId(1);
        routeRecordWS.setName("test_route2");

        // Cross Company
        try {
            capsuleAdminApi.updateRouteRecord(routeRecordWS, PRANCING_PONY_ROUTE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteRouteRecord() {
        // Cross Company
        try {
            capsuleAdminApi.deleteRouteRecord(PRANCING_PONY_ROUTE_ID, 1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testSearchDataTable() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setFilters(new BasicFilter[]{
                new BasicFilter("dialed", Filter.FilterConstraint.EQ, "613")
        });
        criteria.setMax(10);
        // Cross Company
        try {
            capsuleAdminApi.searchDataTable(PRANCING_PONY_ROUTE_ID, criteria);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetRouteTable() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setFilters(new BasicFilter[] {
                new BasicFilter("dialed", Filter.FilterConstraint.EQ, "613")
        });
        criteria.setMax(10);
        // Cross Company
        try {
            capsuleAdminApi.getRouteTable(PRANCING_PONY_ROUTE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetDataTableQuery() {
        // Cross Company
        try {
            oscorpAdminApi.getDataTableQuery(MORDOR_DATA_TABLE_QUERY_ID_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, MORDOR_DATA_TABLE_QUERY_ID_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, MORDOR_ENTITY_ID, ADMIN_LOGIN)));
        }
    }

    @Test
    public void testDeleteDataTableQuery() {
        // Cross Company
        try {
            oscorpAdminApi.deleteDataTableQuery(MORDOR_DATA_TABLE_QUERY_ID_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, MORDOR_DATA_TABLE_QUERY_ID_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, MORDOR_ENTITY_ID, ADMIN_LOGIN)));
        }
    }
}
