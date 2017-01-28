package com.paypal.sdk;

import com.paypal.sdk.http.Environment;
import com.paypal.sdk.model.AccessToken;
import com.paypal.sdk.services.TokenService;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

public class OAuthInjector implements Injector {

	/**
	 * Cached copy of access token
	 *
	 * Visible for testing purposes
	 */
	protected AccessToken mAccessToken;

	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	private TokenService mTokenService;

	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	private String mRefreshToken;

	public OAuthInjector(Environment environment) {
		mTokenService = new TokenService(environment);
	}

	public OAuthInjector(Environment environment, String refreshToken) {
		this(environment);
		mRefreshToken = refreshToken;
	}

	@Override
	public synchronized void inject(HttpRequest request) throws IOException {
		AccessToken token;
		if (mAccessToken != null && !mAccessToken.isExpired()) { // ClientId + ClientSecret auth
			token = mAccessToken;
		} else {
			if (mRefreshToken != null) {
				token = mTokenService.fetchAccessToken(mRefreshToken);
				mAccessToken = token;
			} else {
				token = mTokenService.fetchAccessToken();
				mAccessToken = token;
			}
		}

		request.header("Authorization", "Bearer " + token.accessToken());
	}
}
