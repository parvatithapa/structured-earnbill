package com.sapienter.jbilling.client.suretax;

import java.io.StringReader;

import com.sapienter.jbilling.client.suretax.request.*;
import com.sapienter.jbilling.client.suretax.response.SureAddressResponse;

import com.sapienter.jbilling.client.suretax.responsev1.SuretaxResponseV1;
import com.sapienter.jbilling.client.suretax.responsev2.SuretaxResponseV2;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import com.sapienter.jbilling.client.suretax.response.SuretaxCancelResponse;

public class SuretaxClient {
    Logger log = Logger.getLogger(SuretaxClient.class.getName());

    // public static final String SURETAX_TESTAPI_POST_URL =
    // "https://testapi.taxrating.net/Services/V01/SureTax.asmx/PostRequest";
    // public static final String SURETAX_TESTAPI_CANCEL_POST_URL =
    // "https://testapi.taxrating.net/Services/V01/SureTax.asmx/CancelPostRequest ";
    // public static final String SURETAX_POST_URL =
    // "https://testapi.taxrating.net/Services/V01/SureTax.asmx/PostRequest";
    // public static final String SURETAX_CANCEL_POST_URL =
    // "https://testapi.taxrating.net/Services/V01/SureTax.asmx/CancelPostRequest ";

    public SuretaxResponseV1 calculateAggregateTax(SuretaxRequest request, String url) {
        return request(request, url, SuretaxResponseV1.class, null);
    }

    public SuretaxResponseV2 calculateLineItemTax(SuretaxRequest request, String url) {
        return request(request, url, SuretaxResponseV2.class, null);
    }

    private <T extends IResponse> T request(Object request, String url, Class<T> clazz, ObjectMapper mapper) {
        try {
            HttpClient client = new HttpClient();
            PostMethod post = new PostMethod(url);

            if(mapper == null) {
                mapper = new ObjectMapper();
            }
            String jsonRequestString = mapper.writeValueAsString(request);
            log.debug("Sending suretax json request string: "
                    + jsonRequestString);

            post.addParameter("request", jsonRequestString);

            int respCode = client.executeMethod(post);
            String response = post.getResponseBodyAsString();
            SAXReader saxReader = new SAXReader();
            Document doc = saxReader.read(new StringReader(response));
            log.debug("Suretax response code:" + respCode
                    + ", Suretax response json string:"
                    + doc.getRootElement().getText());
            T stResponse = mapper.readValue(doc.getRootElement()
                    .getText(), clazz);
            stResponse.setJsonString(doc.getRootElement().getText());

            return stResponse;
        } catch (Exception e) {
            log.error("Exception while making a suretax request", e);
            return null;
        }
    }

    public SureAddressResponse getAddressResponse(SureAddressRequest request, String url) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return request(request, url, SureAddressResponse.class, mapper);
    }

    public SuretaxCancelResponse getCancelResponse(
            SuretaxCancelRequest request, String url) {
        return request(request, url, SuretaxCancelResponse.class, null);
    }
}
