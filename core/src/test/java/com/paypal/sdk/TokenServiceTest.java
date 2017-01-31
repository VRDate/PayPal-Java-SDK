package com.paypal.sdk;

import com.paypal.sdk.http.utils.WireMockHarness;
import com.paypal.sdk.model.RefreshToken;
import com.paypal.sdk.services.TokenService;
import com.sun.xml.internal.messaging.saaj.util.Base64;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.testng.Assert.assertEquals;

public class TokenServiceTest extends WireMockHarness {

	private TokenService mTokenService;

	@BeforeMethod
	public void setup() {
		super.setup();
		mTokenService =  new TokenService(environment());
	}

	@Test
	public void TokenService_fetchAccessToken_clientId_fetchesAccessToken() throws IOException {
		stubAccessTokenRequest(simpleAccessToken());

		mTokenService.fetchAccessToken();
		verify(postRequestedFor(urlEqualTo("/v1/oauth2/token"))
				.withRequestBody(equalTo("grant_type=client_credentials"))
				.withHeader("Authorization", equalTo("Basic " + basicHeader()))
				.withHeader("Content-Type", equalTo("application/x-www-form-urlencoded")));
	}

	@Test
	public void TokenService_fetchAccessToken_refreshToken_fetchesAccessToken() throws IOException {
		stubAccessTokenWithRefreshTokenRequest(simpleAccessToken());

		mTokenService.fetchAccessToken("refresh-token");
		verify(postRequestedFor(urlEqualTo("/v1/identity/openidconnect/tokenservice"))
				.withHeader("Authorization", equalTo("Basic " + basicHeader()))
				.withRequestBody(equalTo("grant_type=client_credentials&refreshToken=refresh-token"))
				.withHeader("Content-Type", equalTo("application/x-www-form-urlencoded")));
	}

	@Test
	public void TokenService_fetchRefreshToken_fetchesRefreshToken() throws IOException {
		stubAccessRefreshTokenWithAuthorizationCodeRequest("refresh-token");

		mTokenService.fetchRefreshToken("sample_authorization_code");
		verify(postRequestedFor(urlEqualTo("/v1/identity/openidconnect/tokenservice"))
				.withHeader("Authorization", equalTo("Basic " + basicHeader()))
				.withRequestBody(equalTo("grant_type=authorization_code&code=sample_authorization_code"))
				.withHeader("Content-Type", equalTo("application/x-www-form-urlencoded")));
	}

	@Test
	void TokenService_deserializesRefreshToken() throws IOException {
		HttpRequest<RefreshToken> refreshTokenRequest = new HttpRequest<>("/v1/identity/openidconnect/tokenservice", "POST", RefreshToken.class)
				.baseUrl(environment().baseUrl());

		String refreshResponseString = "{\n" +
				"  \"token_type\": \"Bearer\",\n" +
				"  \"expires_in\": \"28800\",\n" +
				"  \"refresh_token\": \"Refresh-Token-Value\",\n" +
				"  \"access_token\": \"Access-Token-Value\"\n" +
				"}";
		HttpResponse<String> refreshTokenResponse = HttpResponse.<String>builder()
				.result(refreshResponseString).build();

		stub(refreshTokenRequest, refreshTokenResponse);
		RefreshToken refreshToken = mTokenService.fetchRefreshToken("sample_authorization_code");
		assertEquals(refreshToken.refreshToken(), "Refresh-Token-Value");
	}

	private String basicHeader() throws UnsupportedEncodingException {
		byte[] encoded = Base64.encode((environment().getClientId() + ":" + environment().getClientSecret()).getBytes("UTF-8"));
		return new String(encoded);
	}
}
