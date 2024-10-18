package com.sapienter.jbilling.server.login;

import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.JBillingLogFileReader;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Created by nenad on 10/20/16.
 */
@Test(groups = {"web-services", "login"}, testName = "LoginTests")
public class LoginTests {

    private static final Logger logger = LoggerFactory.getLogger(LoginTests.class);

    private JbillingAPI api;
    JBillingLogFileReader logMonitor;

    @BeforeSuite
    private void setUp() throws IOException, JbillingAPIException {
        api = JbillingAPIFactory.getAPI();
        logMonitor = new JBillingLogFileReader();
    }

    @Test
    public void testLoginSuccess() {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost post = postAuthRequest("admin", "123qwe");
            logMonitor.setWatchPoint();
            HttpResponse response = httpClient.execute(post, httpContext());
            String loggedMessages = logMonitor.readLogAsString();
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_MOVED_TEMPORARILY);
            assertTrue(loggedMessages.contains("User admin;1 successfully logged in"));
        } catch (IOException e) {
            logger.error("Error trying to login", e);
        }
    }

    public void testLoginFailure() {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost post = postAuthRequest("admin", "nonvalid");
            logMonitor.setWatchPoint();
            HttpResponse response = httpClient.execute(post, httpContext());
            String loggedMessages = logMonitor.readLogAsString();
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_MOVED_TEMPORARILY);
            assertTrue(loggedMessages.contains("User admin;1 failed to login."));
        } catch (IOException e) {
            logger.error("Error trying to log in", e);
        }
    }

    private HttpPost postAuthRequest(String user, String pass) throws UnsupportedEncodingException {
        HttpPost post = new HttpPost("http://localhost:8080/jbilling/j_spring_security_check");
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("interactive_login", "true"));
        params.add(new BasicNameValuePair("j_username", user));
        params.add(new BasicNameValuePair("j_password", pass));
        params.add(new BasicNameValuePair("j_client_id", "1"));
        post.setEntity(new UrlEncodedFormEntity(params));
        return post;
    }

    private HttpContext httpContext() {
        HttpContext httpContext = new BasicHttpContext();
        CookieStore cookieStore = new BasicCookieStore();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        return httpContext;
    }
}
