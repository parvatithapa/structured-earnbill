package com.sapienter.jbilling.server.pluggableTask.fullcreative.event;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.validator.UrlValidator;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

public class FCWebhookAPICallBuilder {
	
	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(FCWebhookAPICallBuilder.class));
	
	private static final String[] allowedSchemes = {"https"};
	
	private static final UrlValidator urlValidator = new UrlValidator(allowedSchemes);
	
	public static Map<String, Object> makeApiCall(String url, String requestBody, Integer timeout) throws IOException {
		
		Map<String, Object> responseMap = new HashMap<String, Object>();
		
		LOG.debug("Remote Server url : " + url);		
		
		HttpsURLConnection httpsConnection = (HttpsURLConnection) new URL(url).openConnection();			
		httpsConnection.setRequestMethod(RequestMethod.POST.toString());
		httpsConnection.setDoOutput(true);
		httpsConnection.setConnectTimeout(timeout);
		httpsConnection.setInstanceFollowRedirects(false);
		httpsConnection.setRequestProperty("Content-Type", "application/json");
		httpsConnection.setRequestProperty("jbilling_access_token", "jBillingEventWebhook");
		
		OutputStreamWriter writer = new OutputStreamWriter(httpsConnection.getOutputStream());
		writer.write(requestBody);
		writer.close();
		
		if(httpsConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
			StringBuffer response = new StringBuffer();
			BufferedReader reader = new BufferedReader(new InputStreamReader(httpsConnection.getInputStream()));
			String line;
			while((line = reader.readLine()) != null){
				response.append(line);
			}
			
			LOG.debug("Response => " + response.toString());
			
			responseMap = new ObjectMapper().readValue(response.toString(), new TypeReference<Map<String,Object>>() {});
			
		}else{
			responseMap.put("success", false);
			responseMap.put("message", "Got \"" +  httpsConnection.getResponseCode() + "\" error code from remote server.");
		} 
		
		LOG.debug("httpConnection.getResponseCode() " + httpsConnection.getResponseCode());
		
		return responseMap;
	}
	
	public static void validateURL(String remoteURL) throws PluggableTaskException {
		
		if(!urlValidator.isValid(remoteURL)){
			throw new PluggableTaskException("Provided URL must be secured.");
		}
	}
}
