package com.cordys.web.rest;

public class ServiceMapping {
	private String webserviceName;
	private String uriPattern;
	private String httpMethodType;
	private String resourceName;

	public String getWebserviceName() {
		return webserviceName;
	}

	public String getUriPattern() {
		return uriPattern;
	}

	public String getHttpMethodType() {
		return httpMethodType;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setWebserviceName(final String webserviceName) {
		this.webserviceName = webserviceName;
	}

	public void setUriPattern(final String uriPattern) {
		this.uriPattern = uriPattern;
	}

	public void setHttpMethodType(final String httpMethodType) {
		this.httpMethodType = httpMethodType;
	}

	public void setResourceName(final String resourceName) {
		this.resourceName = resourceName;
	}
}
