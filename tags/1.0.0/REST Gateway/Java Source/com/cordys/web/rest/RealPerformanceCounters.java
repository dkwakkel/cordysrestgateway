package com.cordys.web.rest;

import com.eibus.management.IManagedComponent;
import com.eibus.management.counters.CounterFactory;
import com.eibus.management.counters.ITimerEventValueCounter;

public class RealPerformanceCounters implements PerformanceCounters {

	private static final String REQUEST_PROCESSING_COUNTER_NAME = "RequestProcessingCounter";
	private static final String RESPONSE_CREATION_COUNTER_NAME = "ResponseCreationCounter";
	private IManagedComponent gatewayComponent;
	private ITimerEventValueCounter requestProcessing;
	private ITimerEventValueCounter responseCreation;

	public RealPerformanceCounters(IManagedComponent managedComponent) {
		gatewayComponent = managedComponent;
		requestProcessing = (ITimerEventValueCounter) gatewayComponent
				.createPerformanceCounter(REQUEST_PROCESSING_COUNTER_NAME,
						RESTGatewayMessages.REST_REQUEST_PROCESSING_COUNTER,
						CounterFactory.TIMER_EVENT_VALUE_COUNTER);
		responseCreation = (ITimerEventValueCounter) gatewayComponent
				.createPerformanceCounter(RESPONSE_CREATION_COUNTER_NAME,
						RESTGatewayMessages.REST_RESPONSE_CREATION_COUNTER,
						CounterFactory.TIMER_EVENT_VALUE_COUNTER);
	}

	/*
	 * @see com.cordys.web.rest.PerformanceCounters#getStartTime()
	 */
	@Override
	public long getStartTime() {
		return requestProcessing.start();
	}

	/*
	 * @see
	 * com.cordys.web.rest.PerformanceCounters#finishRequestProcessing(long)
	 */
	@Override
	public void finishRequestProcessing(long startTime) {
		requestProcessing.finish(startTime);
	}

	/*
	 * @see com.cordys.web.rest.PerformanceCounters#finishResponseCreation(long)
	 */
	@Override
	public void finishResponseCreation(long startTime) {
		responseCreation.finish(startTime);
	}

	@Override
	public IManagedComponent getGatewayComponent() {
		return gatewayComponent;
	}
}
