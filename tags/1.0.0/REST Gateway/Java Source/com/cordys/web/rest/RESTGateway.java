package com.cordys.web.rest;

import com.eibus.management.IManagedComponent;
import com.eibus.web.gateway.SOAPTransaction;
import com.eibus.web.isapi.ExtensionControlBlock;
import com.eibus.web.isapi.Response;
import com.eibus.web.soap.Gateway;
import com.eibus.xml.nom.Node;
import com.eibus.xml.nom.XMLException;

public class RESTGateway extends Gateway {

	private static final String REST = "com.cordys.web.rest.RestTransaction";
	private PerformanceCounters counters;

	@Override
	public byte[] getRequestXML(final SOAPTransaction soapTransaction) {
		if (counters == null) {
			counters = PerformanceCounters.Factory.getPerformanceCounters(null);
		}
		RestTransaction transaction = new RestTransaction(soapTransaction,
				counters, getOrganizationalContext(soapTransaction));
		soapTransaction.getContext().setAttribute(REST, transaction);
		return transaction.createSOAPRequest();
	}

	@Override
	public void setResponseXML(final SOAPTransaction soapTransaction,
			final byte[] responseXML) {
		ExtensionControlBlock ecb = soapTransaction.getExtensionControlBlock();
		Response response = ecb.getResponse();
		String restResponse = null;
		RestTransaction restTransaction = (RestTransaction) soapTransaction
				.getContext().getAttribute(REST);
		if (restTransaction == null) {
			restResponse = createFaultString(responseXML);
		} else {
			restResponse = restTransaction.createResponse(soapTransaction,
					responseXML);
		}
		response.setHeader("Cache-Control", "no-cache");
		response.setContentType("text/xml");
		response.setHeader("charset", "UTF-8");
		response.write(restResponse);
		finished(ecb);
	}

	@Override
	protected String getManagedComponentType() {
		return "RESTGateway";
	}

	@Override
	protected String getDescription() {
		return "RESTGateway";
	}

	@Override
	protected void setupManagedComponent(IManagedComponent managedComponent) {
		super.setupManagedComponent(managedComponent);
		counters = PerformanceCounters.Factory
				.getPerformanceCounters(managedComponent);
		RestTransaction.initJMXSettings(managedComponent);
	}

	private String createFaultString(byte[] response) {
		String faultString;
		int responseNode = 0;
		try {
			responseNode = DocumentPool.loadXML(response);
			faultString = FaultHandler.createRESTFault(responseNode);
		} catch (XMLException e) {
			faultString = new String(response);
		} finally {
			Node.delete(responseNode);
		}
		return faultString;
	}
}