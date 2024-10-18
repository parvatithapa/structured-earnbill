package com.sapienter.jbilling.rest

import net.javacrumbs.jsonunit.JsonMatchers
import net.javacrumbs.jsonunit.core.Option

import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import java.time.LocalDateTime
import java.time.ZoneId

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vojislav Stanojevikj
 * @since 17-Aug-2016.
 */
final class RestApiHelper {

    private RestApiHelper(){}

    public static final Date DUMMY_TEST_DATE = Date.from(LocalDateTime.of(2010, 4, 5, 0, 0).atZone(ZoneId.systemDefault()).toInstant())

    public static boolean validateResponseHeaders(def response, String headerName, int size, String... values){

        List<String> headers = response.getHeaders(headerName)
        assertTrue("Null value for response headers with name: " + headerName, null != headers)
        assertTrue("Response headers with name: " + headerName +
                ", expected size: " + size + ", actual size: " + headers.size(), headers.size() == size)
        values.each {
            assertTrue("Response header does not contain value: " + it + ".", headers.contains(it))
        }
    }

    public static void validateResponseJson(def response, def jsonString){

        if (!response || !jsonString) {
            fail('Mandatory check fields absent!')
        }
        jsonString = jsonString.replaceAll("\\n+", "")
        assertThat(response.getText(), JsonMatchers.jsonEquals(jsonString).when(Option.IGNORING_ARRAY_ORDER, Option.TREATING_NULL_AS_ABSENT))
    }

    public static boolean validateResponseErrorJSONBody(def response, errorCode, errorMessage, params) {
        def jsonStringFromResponse = response.getText();
        if (errorMessage){
            String lineSep = System.getProperty("line.separator");
            lineSep = lineSep.replace("\n", "\\n").replace("\r", "\\r");
            errorMessage = "${errorMessage}${lineSep}"
        }
        String expectedError = "{\"errorCode\":\"${errorCode}\",\"errorMessages\":\"${errorMessage}\",\"params\":\"${params}\"}"
        jsonStringFromResponse != null && jsonStringFromResponse.equals(expectedError)
    }

    public static void validateResponseJsonString(String responseJson, String jsonString){
        if (!responseJson || !jsonString) {fail('Mandatory check fields absent!')}
        jsonString = jsonString.replaceAll("\\n+", "")
        assertThat(responseJson, JsonMatchers.jsonEquals(jsonString).when(Option.IGNORING_ARRAY_ORDER, Option.TREATING_NULL_AS_ABSENT))
        //following can also be used
        //org.skyscreamer.jsonassert.JSONAssert.assertEquals(response, inputJson, false);
    }

    public static buildJsonHeaders(boolean contentType = true, boolean accept = true){
        def headers = [:]
        if (contentType)
        headers[HttpHeaders.CONTENT_TYPE] = MediaType.APPLICATION_JSON
        if (accept)
        headers[HttpHeaders.ACCEPT] = MediaType.APPLICATION_JSON
        headers
    }

}
