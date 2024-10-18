package com.sapienter.jbilling.rest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import javax.ws.rs.core.Response;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.util.CurrencyWS;

/**
 * @author Nitisha Sahay
 * @since 02-Oct-2018.
 */

@Test(groups = { "rest" }, testName = "CurrencyRestTest")
public class CurrencyRestTest extends RestTestCase {


	private RestOperationsHelper currencyRestHelper;
	
	private static final String DESCRIPTION = "Australian Dollar";
	private static final String SYMBOL = "$";
	private String CODE = "AUD";
	private static final String COUNTRY_CODE = "AU";
	private static final boolean inUse = true;
	private static final String RATE = "1.50";
	private static final String SYS_RATE = "1.288";
	private static final String RESPONSE = "Response can not be null !!";

	@BeforeClass
	public void setup() {
		super.setup("currencies");
		currencyRestHelper = RestOperationsHelper.getInstance("currencies");

	}

	@Test
	public void getCurrencies() {

		ResponseEntity<CurrencyWS[]> currenciesList = restTemplate
				.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null, CurrencyWS[].class);

		assertNotNull(currenciesList, "Currencies list can not be null!!");

		RestValidationHelper.validateStatusCode(currenciesList,
				Response.Status.OK.getStatusCode());
		int initialNumberOfEntities = currenciesList.getBody().length;

		ResponseEntity<Integer> currencyId = restTemplate.sendRequest(REST_URL,
				HttpMethod.POST, postOrPutHeaders,
				RestEntitiesHelper.buildCurrencyMock(DESCRIPTION, SYMBOL, CODE, COUNTRY_CODE , inUse, RATE , SYS_RATE),
				Integer.class);
		currenciesList = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
				getOrDeleteHeaders, null, CurrencyWS[].class);
		assertEquals(initialNumberOfEntities + 1, currenciesList.getBody().length, "Initial number of items did not increase!");

		ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL
				+ currencyId.getBody(), HttpMethod.DELETE, getOrDeleteHeaders,
				null);
		assertNotNull(deletedResponse, "RESPONSE!!");

	}

	@Test
	public void createCurrency() {

		boolean isPresent = false;
		ResponseEntity<Integer> currencyId = restTemplate.sendRequest(REST_URL,
				HttpMethod.POST, postOrPutHeaders,
				RestEntitiesHelper.buildCurrencyMock(DESCRIPTION, SYMBOL, CODE, COUNTRY_CODE , inUse, RATE , SYS_RATE),
				Integer.class);

		assertNotNull(currencyId, "currency Id can not be null!!");
		RestValidationHelper.validateStatusCode(currencyId,
				Response.Status.OK.getStatusCode());

		ResponseEntity<CurrencyWS[]> currenciesList = restTemplate
				.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null, CurrencyWS[].class);
		assertNotNull(currenciesList, "Currencies list can not be null!!");

		CurrencyWS[] currencies = currenciesList.getBody();
		for(CurrencyWS currency: currencies){
			if(currency.getId().equals(currencyId.getBody())){
				isPresent = true;
			}
		}
		assertTrue(isPresent, "Currency is not created in the DB.");
		ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL
				+ currencyId.getBody(), HttpMethod.DELETE, getOrDeleteHeaders,
				null);
		assertNotNull(deletedResponse, RESPONSE);

	}

	@Test
	public void deleteCurrency() {

		ResponseEntity<Integer> currencyId = restTemplate.sendRequest(REST_URL,
				HttpMethod.POST, postOrPutHeaders,
				RestEntitiesHelper.buildCurrencyMock(DESCRIPTION, SYMBOL, CODE, COUNTRY_CODE , inUse, RATE , SYS_RATE),
				Integer.class);

		ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL
				+ currencyId.getBody(), HttpMethod.DELETE, getOrDeleteHeaders,
				null);
		assertNotNull(deletedResponse, RESPONSE);

		RestValidationHelper.validateStatusCode(deletedResponse,
				Response.Status.NO_CONTENT.getStatusCode());
	}

	@Test
	public void updateCurrency() {

		ResponseEntity<Integer> currencyId = restTemplate.sendRequest(REST_URL,
				HttpMethod.POST, postOrPutHeaders,
				RestEntitiesHelper.buildCurrencyMock(DESCRIPTION, SYMBOL, CODE, COUNTRY_CODE , inUse, RATE , SYS_RATE),
				Integer.class);
		CODE = "DEM";
		CurrencyWS updatedMock = RestEntitiesHelper.buildCurrencyMock(DESCRIPTION, SYMBOL, CODE, COUNTRY_CODE , inUse, RATE , SYS_RATE);
		updatedMock.setId(currencyId.getBody());

		ResponseEntity<Integer> updatedResponse = restTemplate.sendRequest(
				REST_URL + currencyId.getBody(), HttpMethod.PUT,
				postOrPutHeaders, updatedMock, Integer.class);

		assertNotNull(updatedResponse, RESPONSE);
		RestValidationHelper.validateStatusCode(updatedResponse,
				Response.Status.OK.getStatusCode());

		ResponseEntity<CurrencyWS[]> currenciesList = restTemplate
				.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null, CurrencyWS[].class);

		assertNotNull(currenciesList, "Currencies list can not be null!!");

		CurrencyWS[] currencies = currenciesList.getBody();
		for(CurrencyWS currency: currencies){
			if(currency.getId().equals(currencyId.getBody())){
				assertEquals(currency.getCode(), CODE);
			}
		}

		ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL
				+ currencyId.getBody(), HttpMethod.DELETE, getOrDeleteHeaders,
				null);
		assertNotNull(deletedResponse, RESPONSE);
	}
}
