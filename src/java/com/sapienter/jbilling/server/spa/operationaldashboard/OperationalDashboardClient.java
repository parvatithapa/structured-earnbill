package com.sapienter.jbilling.server.spa.operationaldashboard;

import com.sapienter.jbilling.common.FormatLogger;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.ssl.SSLContextBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by Matías Cabezas on 04/10/17.
 */
public class OperationalDashboardClient {

    private final static FormatLogger log = new FormatLogger(OperationalDashboardClient.class);
    private String requestURL = "";
    private HttpClient httpClient;
    private Map<String, String> parameters = new HashMap<>();
    private String username;
    private String password;

    public OperationalDashboardClient(String requestURL, Map<String, String> parameters, String username, String password) {
        this.requestURL = requestURL;
        this.httpClient = HttpClients.custom().setSSLSocketFactory(
                getSSLSocketFactory()).build();
        this.parameters = parameters;
        this.username = username;
        this.password = password;
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

    public String sendRequest() {
        try {
            log.info(getRequest());
            String encoding = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            HttpPost request = new HttpPost(requestURL);
            request.setHeader("Authorization", "Basic " + encoding);
            
            List<NameValuePair> requestParameters = new ArrayList<>();

            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                String key = entry.getKey() != null ? entry.getKey().replace(" ", OperationalDashboardRequestGenerator.PARAM_NAME_SEPARATOR) : null;
                String value = entry.getValue();
                requestParameters.add(new BasicNameValuePair(key, value));
            }

            request.setEntity(new UrlEncodedFormEntity(requestParameters, StandardCharsets.UTF_8));
            HttpResponse response = httpClient.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            String responseContent = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

            if (responseCode == HttpStatus.SC_OK) {
                log.info("Succesful operation. Response code: %s", responseCode);
            } else {
                log.info("The operation failed due to bad request. Response code: %s", responseCode);
            }
            String transactionTag = parameters.get(OperationalDashboardRequestGenerator.TRANSACTION_TAG);
            return "RESPSONSE CODE = " + responseCode + ", TR_TAG = " + transactionTag + " " + responseContent;
        } catch (Exception e) {
            String message = "Exception while making an Operational Dashboard request";
            log.error(message, e);
            return message;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    public String getRequest() {
        StringBuilder request = new StringBuilder("Request: ");
        request.append("\nURL: ").append(requestURL).append("\nUser: ").append(username).append("\nPassword: ").append(password);
        parameters.entrySet().stream().forEach(parameter -> {
            request.append("\n");
            request.append(parameter.getKey()).append(" = ").append(parameter.getValue());
        });
        return request.toString();
    }
}
