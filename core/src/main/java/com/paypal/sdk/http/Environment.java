package com.paypal.sdk.http;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public abstract class Environment {

	@Setter(AccessLevel.NONE)
	private String clientId;

	@Setter(AccessLevel.NONE)
	private String clientSecret;

	public Environment(String clientId, String clientSecret) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

    public abstract String baseUrl();

    public static class Production extends Environment {

		public Production(String clientId, String clientSecret) {
			super(clientId, clientSecret);
		}

		@Override
		public String baseUrl() {
			return "https://api.paypal.com";
		}
	}

	public static class Sandbox extends Environment {

		public Sandbox(String clientId, String clientSecret) {
			super(clientId, clientSecret);
		}

		@Override
		public String baseUrl() {
			return "https://api.sandbox.paypal.com";
		}
	}
}
