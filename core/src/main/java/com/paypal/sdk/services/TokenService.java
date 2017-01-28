package com.paypal.sdk.services;

import com.paypal.sdk.HttpRequest;
import com.paypal.sdk.HttpResponse;
import com.paypal.sdk.http.DefaultHttpClient;
import com.paypal.sdk.http.Environment;
import com.paypal.sdk.http.HttpClient;
import com.paypal.sdk.model.AccessToken;
import com.paypal.sdk.model.RefreshToken;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.paypal.sdk.http.Headers.AUTHORIZATION;
import static com.paypal.sdk.http.Headers.CONTENT_TYPE;

public class TokenService {

	private HttpClient mHttpClient;
	private Environment mEnvironment;

	public TokenService(Environment environment) {
		mHttpClient = new DefaultHttpClient();
		mEnvironment = environment;
	}

	public AccessToken fetchAccessToken() throws IOException {
		HttpRequest<AccessToken> accessTokenRequest = getAccessTokenRequest(mEnvironment.baseUrl(), "/v1/oauth2/token", AccessToken.class)
				.requestBody(tokenRequestBody("client_credentials", new HashMap<String, String>()));

		HttpResponse<AccessToken> accessTokenResponse = mHttpClient.execute(accessTokenRequest);
		return accessTokenResponse.result();
	}

	public AccessToken fetchAccessToken(final String refreshToken) throws IOException {
		HttpRequest<AccessToken> accessTokenRequest = getAccessTokenRequest(mEnvironment.baseUrl(), "/v1/identity/openidconnect/tokenservice", AccessToken.class)
				.requestBody(tokenRequestBody("client_credentials", new HashMap<String, String>() {{
					put("refreshToken", refreshToken);
				}}));

		HttpResponse<AccessToken> accessTokenResponse = mHttpClient.execute(accessTokenRequest);
		return accessTokenResponse.result();
	}

	public RefreshToken fetchRefreshToken(final String authorizationCode) throws IOException {
		HttpRequest<RefreshToken> refreshTokenHttpRequest = getAccessTokenRequest(mEnvironment.baseUrl(), "/v1/identity/openidconnect/tokenservice", RefreshToken.class)
				.requestBody(tokenRequestBody("authorization_code", new HashMap<String, String>() {{
					put("code", authorizationCode);
				}}));

		HttpResponse<RefreshToken> refreshTokenResponse = mHttpClient.execute(refreshTokenHttpRequest);
		return refreshTokenResponse.result();
	}

	private <T> HttpRequest<T> getAccessTokenRequest(String baseUrl, String path, Class<T> returnTypeClass) throws IOException {
		return new HttpRequest<>(path, "POST", returnTypeClass)
				.baseUrl(baseUrl)
				.header(AUTHORIZATION, accessTokenRequestHeader())
				.header(CONTENT_TYPE, "application/x-www-form-urlencoded");
	}

	private String accessTokenRequestHeader() throws IOException {
		byte[] encoded = Base64.getEncoder().encode((mEnvironment.getClientId() + ":" + mEnvironment.getClientSecret()).getBytes("UTF-8"));
		return "Basic " + new String(encoded, "UTF-8");
	}

	private String tokenRequestBody(String grantType, Map<String, String> additionalParams) {
		StringBuilder builder = new StringBuilder()
				.append("grant_type=")
				.append(grantType);

		for (Map.Entry<String, String> code : additionalParams.entrySet()) {
			builder.append(String.format("&%s=%s", code.getKey(), code.getValue()));
		}

		return builder.toString();
	}
}
