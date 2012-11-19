package com.cordys.web.rest;

import com.eibus.localization.IStringResource;
import com.eibus.localization.exception.LocalizableException;

public class GenerateServiceMappingsException extends LocalizableException {
	private static final long serialVersionUID = 1L;

	public GenerateServiceMappingsException(final IStringResource resource,
			final Object... insertions) {
		super(resource, insertions);
	}
}
