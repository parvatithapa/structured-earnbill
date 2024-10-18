package com.sapienter.jbilling.client.mcf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.common.FormatLogger;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

/**
 * Created by pablo_galera on 15/02/17.
 */
public class MCFClient {

    private final static FormatLogger log = new FormatLogger(MCFClient.class);

    private MCFResponse request(HttpClient httpClient, MCFRequest request, String url) {
        try {
            HttpPost httpRequest = new HttpPost(url);
            ObjectMapper mapper = new ObjectMapper();
            String jsonRequestString = mapper.writeValueAsString(request);
            log.debug("Sending MCF json request string: "
                    + jsonRequestString);
            StringEntity params =new StringEntity(jsonRequestString);
            httpRequest.addHeader("content-type", "application/json");
            httpRequest.setEntity(params);
            HttpResponse response = httpClient.execute(httpRequest);

            int respCode = response.getStatusLine().getStatusCode();

            String responseContent = IOUtils.toString(response.getEntity().getContent(), "UTF-8");

            log.debug("MCF response code:" + respCode
                    + ", MCF response json string:"
                    + responseContent);
            MCFResponse stResponse = null; mapper.readValue(responseContent, MCFResponse.class);

            if (respCode == 200) {
                stResponse = mapper.readValue(responseContent, MCFResponse.class);
            }
            else {
                log.error("error status " + respCode);
            }
            return stResponse;
        } catch (Exception e) {
            log.error("Exception while making a MCF request", e);
            return null;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    MCFResponse request(MCFRequest request, String url) {
        HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(
                getSSLSocketFactory()).build();
        return request(httpClient,request,url);
    }

    private LayeredConnectionSocketFactory getSSLSocketFactory() {
        SSLContextBuilder builder = new SSLContextBuilder();
        try {
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            return new SSLConnectionSocketFactory(builder.build());
        } catch (Exception e) {
            log.error("Error generating SSLConnectionSocketFactory");
        }
        return null;
    }

}
