package com.paypal.sdk;

import com.paypal.sdk.http.Environment;
import com.paypal.sdk.http.utils.WireMockHarness;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Locale;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.paypal.sdk.http.Headers.*;
import static com.paypal.sdk.http.utils.StubUtils.*;
import static org.testng.Assert.assertEquals;

public class PayPalHttpClientTest extends WireMockHarness {

	private PayPalHttpClient client = null;
	private Environment environment = new Environment.Development("clientId", "clientSecret", baseUrl());

	@BeforeMethod
	public void setup() {
		super.setup();
		client = new PayPalHttpClient(environment);
		stubAccessTokenRequest(simpleAccessToken(), baseUrl());
	}

	@Test
	public void testPayPalHttpClient_execute_setsCommonHeaders() throws IOException {
		HttpRequest<Void> request = new HttpRequest<>("/", "POST", Void.class);
		stub(request, null);

		client.execute(request);
		assertEquals(request.headers().header(ACCEPT_ENCODING), "gzip");
	}

	@Test
	public void testPayPalHttpClient_execute_setsContentTypeHeaderWhenRequestBodyPresent() throws IOException {
		HttpRequest<Void> request = new HttpRequest<>("/", "POST", Void.class);
		request.requestBody("{some json}");
		stub(request, null);

		client.execute(request);

		assertEquals(request.headers().header(CONTENT_TYPE), "application/json");
	}

	@Test
	public void testPayPalHttpClient_execute_setsBaseUrl() throws IOException {
		HttpRequest<Void> request = new HttpRequest<>("/", "POST", Void.class);
		stub(request, null);

		client.execute(request);

		assertEquals(baseUrl() + "/", request.url());
	}

	@Test
	public void testPayPalHttpClient_execute_signsRequest() throws IOException {
		stubAccessTokenRequest(simpleAccessToken(), baseUrl());
		HttpRequest<Void> request = new HttpRequest<>("/", "GET", Void.class)
				.oAuthScope("scope");

		stub(request, null);

		client.execute(request);

		verify(getRequestedFor(urlEqualTo("/"))
		.withHeader("Authorization", equalTo("Bearer sample-access-token")));

	}

	@Test
	public void testPayPalHttpClient_execute_preparesRequest() throws IOException {
		HttpRequest<Void> request = new HttpRequest<>("/", "GET", Void.class);

		stub(request, null);

		client.execute(request);

		verify(getRequestedFor(urlEqualTo("/"))
			.withHeader(ACCEPT_LANGUAGE, equalTo(Locale.getDefault().getLanguage())));
	}

	@Test
	public void testPayPalHttpClient_execute_withCustomInjector_signsRequest() throws IOException {
		HttpRequest<Void> request = new HttpRequest<>("/", "GET", Void.class);

		stub(request, null);

		PayPalHttpClient client = new PayPalHttpClient(environment, (r -> {
			r.header("super-header-key", "super-header-value");
		}));
		client.execute(request);

		verify(getRequestedFor(urlEqualTo("/"))
				.withHeader("super-header-key", equalTo("super-header-value")));
	}
}
