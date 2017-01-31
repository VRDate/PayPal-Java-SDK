package com.paypal.sdk.http.utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.paypal.sdk.HttpRequest;
import com.paypal.sdk.HttpResponse;
import com.paypal.sdk.http.Environment;
import com.paypal.sdk.http.internal.JSONFormatter;
import com.paypal.sdk.model.AccessToken;
import com.paypal.sdk.model.RefreshToken;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class WireMockHarness {

	protected final static int PORT = 8089;
	private WireMockServer wireMockServer = null;
	private Environment mWiremockEnvironment;

	@BeforeMethod
	public void setup() {
		wireMockServer = new WireMockServer(options()
				.port(8089)
				.notifier(new ConsoleNotifier(true)));
		wireMockServer.start();

		WireMock.configureFor("localhost", PORT);

		mWiremockEnvironment = new Environment("clientId", "clientSecret") {
			@Override
			public String baseUrl() {
				return "http://localhost:" + PORT;
			}
		};
	}

	@AfterMethod
	public void teardown() {
		wireMockServer.stop();
	}

	protected Environment environment() {
		return mWiremockEnvironment;
	}

	protected void stub(HttpRequest request, HttpResponse response) {
		MappingBuilder mappingBuilder;

		UrlPathPattern pattern = urlPathEqualTo(request.path());
		String verb = request.verb().toUpperCase();
		if (verb.equals("GET")) {
			mappingBuilder = WireMock.get(pattern);
		} else if (verb.equals("POST")) {
			mappingBuilder = WireMock.post(pattern);
		} else if (verb.equals("PUT")) {
			mappingBuilder = WireMock.put(pattern);
		} else if (verb.equals("DELETE")) {
			mappingBuilder = WireMock.delete(pattern);
		} else {
			throw new RuntimeException("Invalid or no verb passed in request");
		}

		for (String headerKey : request.headers()) {
			mappingBuilder.withHeader(headerKey, equalTo(request.headers().header(headerKey)));
		}

		if (request.requestBody() != null) {
			mappingBuilder.withRequestBody(equalTo(request.serialize()));
		}

		ResponseDefinitionBuilder responseBuilder = aResponse();
		if (response != null) {
			if (response.result() != null) {
				String body = serialize(response.result());
				responseBuilder = responseBuilder
						.withBody(body);
			}

			responseBuilder.withStatus(response.statusCode());

			for (String headerKey : response.headers()) {
				responseBuilder.withHeader(headerKey, response.headers().header(headerKey));
			}

			mappingBuilder.willReturn(responseBuilder);
		}

		stubFor(mappingBuilder);
	}

	protected AccessToken simpleAccessToken() {
		return new AccessToken()
				.accessToken("sample-access-token")
				.expiresIn(3600)
				.tokenType("accessToken");
	}

	/**
	 * Used for stubbing access token requests to the oauth2 endpoint
	 *
	 * @param accessToken the accessToken to be returned in the response
	 */
	protected void stubAccessTokenRequest(AccessToken accessToken) {
		HttpRequest<AccessToken> accessTokenRequest = new HttpRequest<>("/v1/oauth2/token", "POST", AccessToken.class)
				.baseUrl(environment().baseUrl());

		HttpResponse<AccessToken> accessTokenResponse = HttpResponse.<AccessToken>builder()
				.result(accessToken)
				.statusCode(200)
				.build();

		stub(accessTokenRequest, accessTokenResponse);
	}

	/**
	 * Used for stubbing access token requests using a refresh token
	 *
	 * @param accessToken the accessToken to be returned in the response
	 */
	protected void stubAccessTokenWithRefreshTokenRequest(AccessToken accessToken) {
		HttpRequest<RefreshToken> refreshTokenRequest = new HttpRequest<>("/v1/identity/openidconnect/tokenservice", "POST", RefreshToken.class)
				.baseUrl(environment().baseUrl());

		HttpResponse<AccessToken> refreshTokenResponse = HttpResponse.<AccessToken>builder()
				.result(accessToken)
				.statusCode(200)
				.build();

		stub(refreshTokenRequest, refreshTokenResponse);
	}

	/**
	 * Used for stubbing refresh token requests using an authorization code
	 *
	 * @param refreshToken the refresh token to be returned in the response
	 */
	protected void stubAccessRefreshTokenWithAuthorizationCodeRequest(String refreshToken) {
		HttpRequest<RefreshToken> refreshTokenRequest = new HttpRequest<>("/v1/identity/openidconnect/tokenservice", "POST", RefreshToken.class)
				.baseUrl(environment().baseUrl());

		HttpResponse<RefreshToken> refreshTokenResponse = HttpResponse.<RefreshToken>builder()
				.result(new RefreshToken()
						.refreshToken(refreshToken))
				.statusCode(200)
				.build();

		stub(refreshTokenRequest, refreshTokenResponse);
	}

	private String serialize(Object o) {
		if (o instanceof String) {
			return (String) o;
		} else {
			return JSONFormatter.toJSON(o);
		}
	}
}
