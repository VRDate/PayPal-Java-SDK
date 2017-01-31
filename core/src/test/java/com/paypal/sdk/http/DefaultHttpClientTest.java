package com.paypal.sdk.http;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.paypal.sdk.HttpRequest;
import com.paypal.sdk.HttpResponse;
import com.paypal.sdk.http.exceptions.*;
import com.paypal.sdk.http.internal.TLSSocketFactory;
import com.paypal.sdk.http.utils.WireMockHarness;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.http.RequestMethod.*;
import static com.paypal.sdk.http.Headers.*;
import static java.net.HttpURLConnection.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

public class DefaultHttpClientTest extends WireMockHarness {

    private DefaultHttpClient client = null;

    @BeforeMethod
    public void setup() {
    	super.setup();
        client = new DefaultHttpClient();
    }

    @Test(dataProvider = "getErrorCodesWithException")
    public void testHttpClient_execute_throwsProperException(final int statusCode, final Class expectedExceptionClass) throws IOException, IllegalAccessException, InstantiationException {
        HttpRequest<String> request = simpleRequest();
        HttpResponse expectedResponse = HttpResponse.builder().statusCode(statusCode).build();

        stub(request, expectedResponse);

        try {
            client.execute(request);
            fail("We should always be throwing an exception");
        } catch (Exception ex) {
            assertEquals(expectedExceptionClass.getSimpleName(), ex.getClass().getSimpleName());
        }
    }

    @Test(dataProvider = "getSuccessCode")
    public void testHttpClient_execute_returnsSuccess(final int statusCode) throws IOException {
        HttpRequest<String> request = simpleRequest();
        HttpResponse<String> expectedResponse = HttpResponse.<String>builder()
                .statusCode(statusCode)
                .build();

        stub(request, expectedResponse);

        HttpResponse actualResponse = client.execute(request);
        assertEquals(expectedResponse.statusCode(), actualResponse.statusCode());
    }


    @Test(dataProvider = "getVerbs")
    public void testHttpClient_execute_setsVerbFromRequest(RequestMethod requestMethod) throws IOException {
        HttpRequest<String> request = simpleRequest()
				.verb(requestMethod.getName());
        stub(request, null);

        client.execute(request);
        verify(new RequestPatternBuilder(requestMethod, urlEqualTo("/")));
    }

    @Test
    public void testHttpClient_new_setsDefaultValues() throws IOException {
        HttpRequest<String> request = simpleRequest();
        stub(request, null);

        client.execute(request);

        verify(getRequestedFor(urlEqualTo("/"))
                .withHeader("User-Agent", equalTo("Java HTTP/1.1")));
    }

    @Test
    public void testHttpClient_setUserAgent_setsUserAgentInRequest() throws IOException {
        client.setUserAgent("Test User Agent");
        HttpRequest<String> request = simpleRequest();
        stub(request, null);

        client.execute(request);

        verify(getRequestedFor(urlEqualTo("/"))
                .withHeader("User-Agent", equalTo("Test User Agent")));
    }

    @Test
    public void testHttpClient_usesDefaultSSLSocketFactoryWhenNoFactoryIsSet()
            throws IOException, NoSuchFieldException, IllegalAccessException {

        Field sslSocketFactory = client.getClass().getDeclaredField("mSSLSocketFactory");
        sslSocketFactory.setAccessible(true);
        assertTrue(sslSocketFactory.get(client) instanceof TLSSocketFactory);
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "SSLSocketFactory was not set or failed to initialize")
    public void testHttpClient_execute_postsErrorForHttpsRequestsWhenSSLSocketFactoryIsNull()
            throws InterruptedException, IOException {
        client.setSSLSocketFactory(null);

        HttpRequest<String> request = simpleRequest()
				.baseUrl("https://example.com");

		client.execute(request);
    }

    @Test
    public void testHttpClient_getConnection_usesSSLSocketFactory() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        SSLSocketFactory sslSocketFactory = mock(SSLSocketFactory.class);
		client.setSSLSocketFactory(sslSocketFactory);

        HttpRequest<String> request = simpleRequest()
				.baseUrl("https://example.com");
        HttpURLConnection connection = client.getConnection(request);

        assertEquals(sslSocketFactory, ((HttpsURLConnection) connection).getSSLSocketFactory());
    }

    @Test
    public void testHttpClient_getConnection_doesNotUseSSLSocketFactoryForNonHttpsRequests() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        HttpRequest<String> request = simpleRequest();
        HttpURLConnection connection = client.getConnection( request);

        assertFalse(connection instanceof HttpsURLConnection);
    }

    @Test
    public void testHttpClient_getConnection_usesRequestHeaderValueOverItsOwn() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
		client.setUserAgent("Client User Agent");

        HttpRequest<String> request = simpleRequest();
        request.headers().header(USER_AGENT, "Request User Agent");

        HttpURLConnection connection = client.getConnection(request);

        assertEquals("Request User Agent", connection.getRequestProperty("User-Agent"));
    }

    @Test
    public void testHttpClient_getConnection_returnsNullIfNoContentTypeSet() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        HttpRequest<String> request = simpleRequest();
        HttpURLConnection connection = client.getConnection( request);

        assertNull(connection.getRequestProperty(CONTENT_TYPE.toString()));
    }

    @Test
    public void testHttpClient_getConnection_setsDefaultConnectTimeoutWhenNoTimeoutIsSet() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        HttpRequest<String> request = simpleRequest();
        HttpURLConnection connection = client.getConnection( request);

        assertEquals(30000, connection.getConnectTimeout());
    }

    @Test
    public void testHttpClient_getConnection_setsConnectTimeout() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		client.setConnectTimeout(50000);

        HttpRequest<String> request = simpleRequest();
        HttpURLConnection connection = client.getConnection( request);
        assertEquals(50000, connection.getConnectTimeout());
    }

    @Test
    public void testHttpClient_getConnection_setsDefaultReadTimeoutWhenNoTimeoutIsSet() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        HttpRequest<String> request = simpleRequest();
        HttpURLConnection connection = client.getConnection( request);

        assertEquals(30000, connection.getReadTimeout());
    }

    @Test
    public void testHttpClient_getConnection_setsReadTimeoutWhenTimeoutIsSet() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		client.setReadTimeout(50000);

        HttpRequest<String> request = simpleRequest();
        HttpURLConnection connection = client.getConnection( request);

        assertEquals(50000, connection.getReadTimeout());
    }

    @Test
    public void testHttpClient_execute_setsHeadersFromRequest() throws IOException {
        HttpRequest<String> request = simpleRequest()
                .header("Key1", "Value1")
				.header(CONTENT_TYPE, "application/xml");

        stub(request, null);

        client.execute(request);
        verify(getRequestedFor(urlEqualTo("/"))
                .withHeader("Key1", equalTo("Value1"))
                .withHeader("Content-Type", equalTo("application/xml")));
    }

    @Test
    public void testHttpClient_execute_writesDataFromRequestIfPresent() throws IOException {
        HttpRequest<String> request = simpleRequest()
				.verb("POST")
                .requestBody("some data");
        stub(request, null);

        client.execute(request);
        verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(equalTo("some data")));
    }

    @Test
    public void testHttpClient_execute_doesNotwriteDataFromRequestIfNotPresent() throws IOException {
        HttpRequest<String> request = simpleRequest()
				.verb("POST");
        stub(request, null);

        client.execute(request);
        verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(equalTo("")));
    }

    @Test
	public void testDefaultHttpClient_execute_setsCommonHeaders() throws IOException {
	    HttpRequest<String> request = simpleRequest();
		stub(request, null);

	    client.execute(request);
        assertEquals(request.headers().header(USER_AGENT), "Java HTTP/1.1");
        assertEquals(request.headers().header(ACCEPT_LANGUAGE), Locale.getDefault().getLanguage());
    }

    @Test
    public void testDefaultHttpClient_execute_doesNotSetCommonHeadersIfPresent() throws IOException {
        HttpRequest<String> request = simpleRequest();
        request.header(USER_AGENT, "Custom User Agent");
        request.header(ACCEPT_LANGUAGE, "custom");

        stub(request, null);

        client.execute(request);

        assertEquals(request.headers().header(USER_AGENT), "Custom User Agent");
        assertEquals(request.headers().header(ACCEPT_LANGUAGE), "custom");
    }

    @Test
	public void testDefaultHttpClient_addInjector_usesCustomInjectors() throws IOException {
    	client.addInjector(request -> {
			request.header(PAYPAL_REQUEST_ID, "request-id");
		});

		HttpRequest<String> request = simpleRequest();
		stub(request, null);

		client.execute(request);

		assertEquals(request.headers().header(PAYPAL_REQUEST_ID), "request-id");
	}

	@Test
	public void testDefaultHttpClient_addInjector_withNull_doestNotAddNullInjector() {
		client.addInjector(null);
		assertEquals(1, client.mInjectors.size());
	}

    @Test
    public void testDefaultHttpClient_applyHeadersFromRequest_SetsHeaders() throws IOException {
        HttpRequest<String> request = simpleRequest();
        request.header(USER_AGENT, "Custom User Agent");
        HttpURLConnection connection = (HttpURLConnection) new URL(request.url()).openConnection();

        client.applyHeadersFromRequest(request, connection);
        assertEquals(connection.getRequestProperty(USER_AGENT.toString()), "Custom User Agent");
    }

    @Test
    public void testDefaultHttpClient_parseResponseHeaders_returnsParsedHeaders() throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getHeaderField(anyInt())).thenReturn("value");
        when(connection.getHeaderFieldKey(anyInt())).thenReturn("key");

        when(connection.getHeaderFields()).thenReturn(new HashMap<String, List<String>>() {{
            put("key", new ArrayList<String>() {{ add("value"); }});
        }});

        Headers actualResponse = client.parseResponseHeaders(connection);
        assertEquals("value", actualResponse.header("key"));
    }

	@DataProvider(name = "getVerbs")
    public Object[][] getVerbs() {
        return new Object[][]{
                {GET},
                {POST},
                {PUT},
                {DELETE}
        };
    }

    @DataProvider(name = "getSuccessCode")
    public Object[][] getSuccessCode() {
        return new Object[][]{
                {HTTP_OK},
                {HTTP_CREATED},
                {HTTP_ACCEPTED},
				{HTTP_RESET},
				{HTTP_NO_CONTENT},
        };
    }

    @DataProvider(name = "getErrorCodesWithException")
    public Object[][] getErrorCodesWithException() {
        return new Object[][]{
                {HTTP_UNAUTHORIZED, AuthenticationException.class},
                {HTTP_FORBIDDEN, AuthorizationException.class},
                {400, BadRequestException.class},
                {422, UnprocessableEntityException.class},
                {426, UpgradeRequiredException.class},
                {429, RateLimitException.class},
                {HTTP_INTERNAL_ERROR, InternalServerException.class},
                {HTTP_UNAVAILABLE, DownForMaintenanceException.class}
        };
    }

    private HttpRequest<String> simpleRequest() {
    	return new HttpRequest<>("/", "GET", String.class)
				.baseUrl(environment().baseUrl());
	}
}
