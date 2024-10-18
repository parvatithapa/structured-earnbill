package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.common.Util;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Created by marcolin on 14/04/16.
 */
public class MetricsHelper {

    public static final String metricUrl = Util.getSysProp("metric.server.url");

    public enum MetricType {
        CDR_READ,
        JMR_READ,
        ORDER_CREATED
    }

    public static void log(String message, String machine, String type) {
        if (metricUrl != null) {
            HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead

            try {
                HttpPost postRequest = new HttpPost(metricUrl);

                StringEntity input = new StringEntity(
                        "{\"id\":\"\"," +
                                "\"description\":\"" + message + "\"," +
                                "\"machine\":\"" + machine+ "\"," +
                                "\"type\":\"" + type + "\"" +
                                "}");
                input.setContentType("application/json");
                postRequest.setEntity(input);
                HttpResponse response = httpClient.execute(postRequest);

                if (response.getStatusLine().getStatusCode() != 201) {
                    throw new RuntimeException("Failed : HTTP error code : "
                            + response.getStatusLine().getStatusCode());
                }
                response.getEntity().getContent();
                httpClient.getConnectionManager().shutdown();
            }catch (Exception ex) {}
        }
    }
}
