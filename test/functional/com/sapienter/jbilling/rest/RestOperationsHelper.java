package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.util.RemoteContext;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * @author Vojislav Stanojevikj
 * @since 13-Oct-2016.
 */
final class RestOperationsHelper {

    private static final RestConfig restConfig = RemoteContext.getBean("restConfig");
    private static final RestConfig resellerRestConfig = RemoteContext.getBean("resellerRestConfig");

    private final String fullRestUrl;
    private final HttpHeaders authHeaders;
    private final HttpHeaders resellerAuthHeaders;

    private RestOperationsHelper(String fullRestUrl, boolean authHeaders){
        this.fullRestUrl = fullRestUrl;
        this.authHeaders = authHeaders ? createAuthHeaders(restConfig.getAuthUsername(), restConfig.getAuthPassword()) : new HttpHeaders();
        this.resellerAuthHeaders = authHeaders ? createAuthHeaders(resellerRestConfig.getAuthUsername(), resellerRestConfig.getAuthPassword()) : new HttpHeaders();
    }

    public static RestOperationsHelper getInstance(String urlResourceName){
        return new RestOperationsHelper(appendToRestURL(restConfig.getRestUrl(), urlResourceName), true);
    }

    public static RestOperationsHelper getInstanceWithoutAuthHeaders(String urlResourceName){
        return new RestOperationsHelper(appendToRestURL(restConfig.getRestUrl(), urlResourceName), false);
    }

    private static String appendToRestURL(String baseUrl, String urlResourceName){
    	if(urlResourceName!=null){
    		return new StringBuilder(baseUrl)
            .append(urlResourceName)
            .append("/").toString();
    	}else{
    		return new StringBuilder(baseUrl).toString();
    	}
    }

    private HttpHeaders createAuthHeaders(String username, String password){
        return new HttpHeaders(){
            {
                String auth = username + ":" + password;
                byte[] encodedAuth = Base64.encodeBase64(
                        auth.getBytes(Charset.forName("US-ASCII")));
                String authHeader = "Basic " + new String(encodedAuth);
                set("Authorization", authHeader);
            }
        };
    }

    public String getFullRestUrl() {
        return fullRestUrl;
    }

    public HttpHeaders getAuthHeaders(){
        return HttpHeaders.readOnlyHttpHeaders(authHeaders);
    }

    public HttpHeaders getResellerAuthHeaders(){
        return HttpHeaders.readOnlyHttpHeaders(resellerAuthHeaders);
    }

    public static HttpHeaders getJSONHeaders(boolean accept, boolean contentType){
        HttpHeaders jsonHeaders = new HttpHeaders();
        if (accept)
        jsonHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        if (contentType)
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        return HttpHeaders.readOnlyHttpHeaders(jsonHeaders);
    }

    public static HttpHeaders appendHeaders(HttpHeaders authHeaders, HttpHeaders requestHeaders){
        if (null == authHeaders){
            return null;
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.putAll(authHeaders);
        if (null != requestHeaders && !requestHeaders.isEmpty()){
            httpHeaders.putAll(requestHeaders);
        }
        return HttpHeaders.readOnlyHttpHeaders(httpHeaders);
    }


}
