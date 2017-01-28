package com.paypal.sdk;

import com.paypal.sdk.http.DefaultHttpClient;
import com.paypal.sdk.http.Environment;
import com.paypal.sdk.http.HttpClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import static com.paypal.sdk.http.Headers.ACCEPT_ENCODING;
import static com.paypal.sdk.http.Headers.CONTENT_TYPE;

@Slf4j
public class PayPalHttpClient extends DefaultHttpClient implements HttpClient {

    private Environment mEnvironment;

    public PayPalHttpClient(Environment environment) {
    	this(environment, new OAuthInjector(environment));
    }

    public PayPalHttpClient(Environment environment, Injector authInjector) {
    	super();
		mEnvironment = environment;
    	addInjector(authInjector);
    	addInjector(this::injectBaseUrl);
		addInjector(this::injectStandardHeaders);
	}

	@Override
	protected String getUserAgent() {
    	return "Java PayPalHttpClient"; // TODO: Return SDK version as part of this user agent.
	}

	private void injectBaseUrl(HttpRequest request) throws IOException {
		if (request.baseUrl() == null) {
			request.baseUrl(mEnvironment.baseUrl());
		}
	}

	private void injectStandardHeaders(HttpRequest request) throws IOException {
		if (request.requestBody() != null) {
			request.headers().headerIfNotPresent(CONTENT_TYPE, "application/json");
		}

		request.headers().headerIfNotPresent(ACCEPT_ENCODING, "gzip");
	}
}
