package com.cordys.web.rest;

import com.eibus.management.IManagedComponent;

public class DummyPerformanceCounters implements PerformanceCounters {

	@Override
	public long getStartTime() {
		return 0;
	}

	@Override
	public void finishRequestProcessing(long startTime) {
	}

	@Override
	public void finishResponseCreation(long startTime) {
	}

	@Override
	public IManagedComponent getGatewayComponent() {
		return null;
	}

}
