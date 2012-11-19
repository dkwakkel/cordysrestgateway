package com.cordys.web.rest;

import com.eibus.management.IManagedComponent;

public interface PerformanceCounters {

	public abstract long getStartTime();

	public abstract void finishRequestProcessing(long startTime);

	public abstract void finishResponseCreation(long startTime);

	public abstract IManagedComponent getGatewayComponent();

	public static class Factory {
		public static PerformanceCounters getPerformanceCounters(
				IManagedComponent managedComponent) {
			return managedComponent == null ? new DummyPerformanceCounters()
					: new RealPerformanceCounters(managedComponent);
		}
	}

}