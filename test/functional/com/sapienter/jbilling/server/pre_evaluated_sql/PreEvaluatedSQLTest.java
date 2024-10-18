package com.sapienter.jbilling.server.pre_evaluated_sql;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.sql.api.*;
import com.sapienter.jbilling.server.sql.api.db.*;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
import com.sapienter.jbilling.server.util.JBillingTestUtils;

@Test(groups = { "pre-evaluated-sql" }, testName = "PreEvaluatedSQLTest")
public class PreEvaluatedSQLTest {

	private static final Logger logger = LoggerFactory.getLogger(PreEvaluatedSQLTest.class);
	private JbillingAPI api;
	private QueryParameterWS userName;
	
	@org.testng.annotations.BeforeClass
	protected void setUp() throws Exception {
		api = JbillingAPIFactory.getAPI();
		userName = new QueryParameterWS("userName", ParameterType.STRING, "admin");
	}
	
	@Test
	public void test001FetchResultFromDb() {
		String queryCode = "Test-01";
		QueryResultWS result = getQueryResult(queryCode, new QueryParameterWS[]{userName}, 4, 0);
		assertNotNull("result can not be null ",result);
	}
	
	@Test
	public void test002InvalidQueryCode() {
		String queryCode = "Test-02";
		try {
			QueryResultWS result = getQueryResult(queryCode, new QueryParameterWS[]{userName}, 4, 0);
			fail("Exception expected");
		} catch(SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "You have passed Invalid Query Code: " +queryCode);
		}
	}
	
	@Test
	public void test003InvalidQueryParameter() {
		String queryCode = "Test-01";
		QueryParameterWS wrongParameter = new QueryParameterWS("UserName", ParameterType.STRING, "admin");
		try {
			QueryResultWS result = getQueryResult(queryCode, new QueryParameterWS[]{wrongParameter}, 4, 0);
			fail("Exception expected");
		} catch(SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "You Have Passed Null or Invalid Parameter "+wrongParameter.getParameterName());
		}
	}
	
	@Test
	public void test004QueryWithEmptyParameter() {
		String queryCode = "Test-01";
		try {
			QueryResultWS result = getQueryResult(queryCode, new QueryParameterWS[]{}, 4, 0);
			fail("Exception expected");
		} catch(SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "Expected Parameters Was: "+1 +" But Was: "+0);
		}
	}
	
	@Test
	public void test005QueryWithNullParameter() {
		String queryCode = "Test-01";
		try {
			QueryResultWS result = getQueryResult(queryCode, null, 4, 0);
			fail("Exception expected");
		} catch(SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "Parameters are required.");
		}
	}
	
	@Test
	public void test006QueryWithWrongParameterType() {
		String queryCode = "Test-01";
		QueryParameterWS wrongParameter = new QueryParameterWS("userName", ParameterType.INTEGER, "admin");
		try {
			QueryResultWS result = getQueryResult(queryCode, new QueryParameterWS[]{wrongParameter}, 4, 0);
			fail("Exception expected");
		} catch(SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "Incorrect Type for  "+wrongParameter.getParameterName()+ " Expected Type: "+userName.getParameterType() +" But Type Was: "+wrongParameter.getParameterType());
		}
	}
	
	@Test
	public void test007LimitParameter() {
		String queryCode = "Test-01";
		QueryResultWS result = getQueryResult(queryCode, new QueryParameterWS[]{userName}, 2, 0);
		assertNotNull("getQueryResult() API Failed", result);
		assertEquals("Row Count Should Be", new Long(2), new Long(result.getRowCount().toString()));

	}
	
	@Test
	public void test008OffsetParameter() {
		String queryCode = "Test-01";
		QueryResultWS result = getQueryResult(queryCode, new QueryParameterWS[]{userName}, 2, 0);
		assertNotNull("getQueryResult() API Failed", result);
		assertEquals("Row Count Should Be", new Long(2), new Long(result.getRowCount().toString()));
		String lastUserId = result.getColumnValuesByColumnName("id").get(1);
		logger.debug("lastUserId:::::::::::::{}", lastUserId);
		result = getQueryResult(queryCode, new QueryParameterWS[]{userName}, 2, 2);
		assertNotNull("getQueryResult() API Failed", result);
		assertEquals("Row Count Should Be", new Long(2), new Long(result.getRowCount().toString()));
		String latestUserId = result.getColumnValuesByColumnName("id").get(1);
		logger.debug("Latest UserId::::::::::{}", latestUserId);
		assertTrue("Offset is not working", !lastUserId.equals(latestUserId));

	}
	
	private QueryResultWS getQueryResult(String queryCode, QueryParameterWS[] parameters, int limit, int offSet) {
		QueryResultWS resultWS = api.getQueryResult(queryCode, parameters, limit, offSet);
		return resultWS;
	}
}
