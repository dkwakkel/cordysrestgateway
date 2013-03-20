package com.cordys.web.rest;

import java.io.UnsupportedEncodingException;

import com.eibus.connector.nom.Connector;
import com.eibus.directory.soap.DirectoryException;
import com.eibus.exception.ExceptionGroup;
import com.eibus.exception.TimeoutException;
import com.eibus.management.IManagedComponent;
import com.eibus.soap.fault.Fault;
import com.eibus.util.logger.CordysLogger;
import com.eibus.web.gateway.SOAPTransaction;
import com.eibus.web.isapi.Request;
import com.eibus.web.util.Util;
import com.eibus.xml.nom.Node;
import com.eibus.xml.nom.XMLException;
import com.eibus.xml.xpath.XPath;
import com.eibus.xml.xpath.XPathMetaInfo;
import com.eibus.xml.xpath.XSLT;

public class RestTransaction {

	private static final String REST = "restful";
	private static final String CORDYS_URL = "/cordys";
	private static final CordysLogger logger = CordysLogger
			.getCordysLogger(RestTransaction.class);
	private static Connector connector;

	private final XPathMetaInfo oMeta = new XPathMetaInfo();
	private final SOAPTransaction soapTransaction;
	private final String requestBody;
	private final String organizationDN;
	private final String methodType;
	private final String resourceURI;
	private final String[] parameters;
	private final String resourceName;
	private final PerformanceCounters counters;

	private String methodXML;
	private String uriPattern;
	private byte[] requestBytes;

	public RestTransaction(final SOAPTransaction soapTransaction,
			final PerformanceCounters counters, final String organizationDN) {
		this.counters = counters;
		oMeta.addNamespaceBinding("soap",
				"http://schemas.xmlsoap.org/soap/envelope/");
		oMeta.addNamespaceBinding("xmlst",
				"http://schemas.cordys.com/1.0/xmlstore");
		this.soapTransaction = soapTransaction;
		Request request = this.soapTransaction.getExtensionControlBlock()
				.getRequest();
		requestBody = new String(request.binaryRead());
		this.organizationDN = organizationDN;
		methodType = request.getMethod();
		final String resourcePath = getResourcePath(request.getPathInfo());
		int startIndex = 0;
		int endIndex = resourcePath
				.indexOf("/com.cordys.web.rest.RESTGateway.wcp");
		if (startIndex >= endIndex) {
			logger.error(new RESTGatewayException(
					RESTGatewayMessages.REST_RESOURCE_NOT_FOUND),
					RESTGatewayMessages.REST_RESOURCE_NOT_FOUND);
			soapTransaction.raiseSOAPFault(Fault.Codes.SERVER, 500,
					RESTGatewayMessages.REST_RESOURCE_NOT_FOUND,
					new String[] { "" });
		}
		resourceURI = resourcePath.substring(startIndex, endIndex);
		parameters = resourceURI.split("/");
		if (resourceURI.contains("/")) {
			resourceName = resourceURI.substring(0, resourceURI.indexOf("/"));
			uriPattern = "/" + resourceName;
			for (int i = 0; i < parameters.length - 1; i++) {
				uriPattern += "/{" + i + "}";
			}
		} else {
			resourceName = resourceURI;
			uriPattern = "/" + resourceName;
		}
		if (resourceName.isEmpty()) {
			String error = "Client Error Resource " + resourceName
					+ " not available";
			logger.error(new RESTGatewayException(
					RESTGatewayMessages.REST_RESOURCE_NOT_FOUND, error),
					RESTGatewayMessages.REST_RESOURCE_NOT_FOUND, error);
			soapTransaction.raiseSOAPFault(Fault.Codes.SERVER, 500,
					RESTGatewayMessages.REST_RESOURCE_NOT_FOUND,
					new String[] { error });
		}
		if (logger.isDebugEnabled()) {
			logger.debug(RESTGatewayMessages.REST_GATEWAY_REQUEST, resourceURI);
		}
	}

	/*
	 * The logic inside this method can break if the Cordys URI protocol changes
	 * To avoid this the Gateway framework should provide APIs to abstract URI
	 * parsing by consumers
	 */
	// TODO: Switch to Gateway APIs when they become available
	private String getResourcePath(final String uri) {
		String resourcePath = null;
		String tokens[] = null;
		String restToken = null;
		if (uri.startsWith(CORDYS_URL)) {
			// When the URI is like /cordys/restful/resource/id
			tokens = uri.split("/", 4);
			if (tokens.length == 4) {
				restToken = tokens[2];
				resourcePath = tokens[3];
			}
		} else {
			// When the URI is like /home/organization/restful/resource/id
			tokens = uri.split("/", 5);
			if (tokens.length == 5) {
				restToken = tokens[3];
				resourcePath = tokens[4];
			}
		}

		if (Util.isEmpty(resourcePath) || !REST.equals(restToken)) {
			logger.error(new RESTGatewayException(
					RESTGatewayMessages.REST_RESOURCE_NOT_FOUND, uri),
					RESTGatewayMessages.REST_RESOURCE_NOT_FOUND, uri);
			soapTransaction.raiseSOAPFault(Fault.Codes.SERVER, 500,
					RESTGatewayMessages.REST_RESOURCE_NOT_FOUND,
					new String[] { uri });
		}
		return resourcePath;
	}

	public byte[] createSOAPRequest() {
		long startTime = counters.getStartTime();
		int mappingXML = 0;
		int soapRequest = 0;
		try {
			String key = "Cordys/servicemapping/resttosoap/" + resourceName
					+ ".xml";
			mappingXML = getMappingInfo(key);
			soapRequest = generateSoapRequest(mappingXML);
			if (logger.isDebugEnabled()) {
				logger.debug(RESTGatewayMessages.REST_GATEWAY_SOAPREQUEST,
						resourceURI, Node.writeToString(soapRequest, true));
			}
			counters.finishRequestProcessing(startTime);
			requestBytes = Node.write(soapRequest, false);
			return requestBytes;
		} finally {
			Node.delete(Node.getRoot(mappingXML));
			Node.delete(Node.getRoot(soapRequest));
		}
	}

	private int generateSoapRequest(final int mappingXML) {
		boolean deleteResponse = true;
		int soapRequestNode = 0;
		int payloadNode = 0;
		int httpBodyNode = 0;
		String error = "Client Error : Invalid Request for resource "
				+ resourceURI;
		int methodNode = 0;
		try {
			methodNode = Node.unlink(XPath.getFirstMatch(".//" + methodType,
					oMeta, mappingXML));
			methodXML = Node.writeToString(methodNode, false);
			if (XPath.getFirstMatch("WSOperation[@URIPattern='" + uriPattern
					+ "']", oMeta, methodNode) == 0) {
				try {
					error = "Client Error : ReST Resource " + resourceURI
							+ " Not Found.";
					throw new RESTGatewayException(
							RESTGatewayMessages.REST_RESOURCE_NOT_FOUND, error);
				} catch (RESTGatewayException e) {
					logger.error(e,
							RESTGatewayMessages.REST_RESOURCE_NOT_FOUND, error);
					soapTransaction.raiseSOAPFault(Fault.Codes.SERVER, 500,
							RESTGatewayMessages.REST_RESOURCE_NOT_FOUND,
							new String[] { error }, e);
				}
			}
			int prefixesNode = XPath.getFirstMatch(
					".//Resource/NamespacePrefixes", oMeta, mappingXML);
			setPrefixesToPayload(prefixesNode);

			if (logger.isDebugEnabled()) {
				logger.debug(methodType + " Webservice XML Template : "
						+ Node.writeToString(methodNode, true));
			}
			String webserviceNamespace = Node.getDataWithDefault(
					XPath.getFirstMatch("WSOperation[@URIPattern='"
							+ uriPattern + "']/Namespace", oMeta, methodNode),
					"");
			byte[] soapEnv = "<SOAP:Envelope xmlns:SOAP=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP:Body xmlns:SOAP=\"http://schemas.xmlsoap.org/soap/envelope/\"></SOAP:Body></SOAP:Envelope>"
					.getBytes();
			soapRequestNode = DocumentPool.loadXML(soapEnv);
			if (logger.isDebugEnabled()) {
				logger.debug("URI Pattern : " + uriPattern);
			}

			if (methodType.equalsIgnoreCase("PUT")
					|| methodType.equalsIgnoreCase("POST")) {
				int xsltNode = XPath.getFirstMatch("WSOperation[@URIPattern='"
						+ uriPattern
						+ "']/Input/Filter[Type='xslt']/Value/stylesheet",
						oMeta, methodNode);
				if (xsltNode == 0) {
					error = "Client Error-400: XSLT not available for transformation for resource "
							+ resourceURI;
					throw new RESTGatewayException(
							RESTGatewayMessages.REST_GATEWAY_BAD_REQUEST, error);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Input XSLT Node from xml template : \n"
							+ Node.writeToString(xsltNode, true));
				}
				if (requestBody.equalsIgnoreCase("")) {
					error = "Client Error-400: Invalid Request Body for resource "
							+ resourceURI;
					throw new RESTGatewayException(
							RESTGatewayMessages.REST_GATEWAY_BAD_REQUEST, error);
				}
				byte[] requestBytes = requestBody.getBytes();
				httpBodyNode = DocumentPool.loadXML(requestBytes);
				payloadNode = transformToXML(httpBodyNode, xsltNode);
				Node.setNSDefinition(payloadNode, null, webserviceNamespace);
				Node.removeAttribute(payloadNode, "xmlns:sch");
				if (logger.isDebugEnabled()) {
					logger.debug("Input Payload : \n"
							+ Node.writeToString(payloadNode, true));
				}
			} else if (methodType.equalsIgnoreCase("GET")
					|| methodType.equalsIgnoreCase("DELETE")) {
				payloadNode = Node.getFirstElement(XPath.getFirstMatch(
						"WSOperation[@URIPattern='" + uriPattern
								+ "']/SOAPRequest", oMeta, methodNode));
			}

			int inputElementsNode = XPath.getFirstMatch(
					"WSOperation[@URIPattern='" + uriPattern + "']/Input",
					oMeta, methodNode);
			int inputElements[] = XPath.getMatchingNodes("Element", oMeta,
					inputElementsNode);
			for (int inputElement : inputElements) {
				String elementXpath = Node.getDataWithDefault(
						XPath.getFirstMatch("Name", oMeta, inputElement), "");
				String elementValue = Node.getDataWithDefault(
						XPath.getFirstMatch("Value", oMeta, inputElement), "");
				if (elementValue.contains("$URL$")) {
					if (parameters.length > 1) {
						String tempVal = elementValue.substring(
								elementValue.indexOf("{") + 1,
								elementValue.indexOf("}"));
						elementValue = parameters[Integer.parseInt(tempVal) + 1];
					}
					if (methodType.equalsIgnoreCase("GET")
							|| methodType.equalsIgnoreCase("DELETE")) {
						Node.setDataElement(XPath.getFirstMatch(".."
								+ elementXpath, oMeta, payloadNode), "",
								elementValue);
					} else {
						Node.setDataElement(XPath.getFirstMatch(elementXpath,
								oMeta, payloadNode), "", elementValue);
					}
				}
			}
			Node.appendToChildren(payloadNode, payloadNode,
					XPath.getFirstMatch(".//soap:Body", oMeta, soapRequestNode));
			payloadNode = 0;
			if (logger.isDebugEnabled()) {
				logger.debug("Input Payload : \n"
						+ Node.writeToString(payloadNode, true));
			}
			deleteResponse = false;
		} catch (XMLException e) {
			logger.error(e, RESTGatewayMessages.REST_GATEWAY_BAD_REQUEST);
			soapTransaction.raiseSOAPFault(Fault.Codes.SERVER, 500,
					RESTGatewayMessages.REST_GATEWAY_BAD_REQUEST,
					new String[] { error }, e);
		} catch (RESTGatewayException e) {
			logger.error(e, RESTGatewayMessages.REST_GATEWAY_BAD_REQUEST);
			soapTransaction.raiseSOAPFault(Fault.Codes.SERVER, 500,
					RESTGatewayMessages.REST_GATEWAY_BAD_REQUEST,
					new String[] { error }, e);
		} finally {
			if (deleteResponse) {
				Node.delete(Node.getRoot(soapRequestNode));
			}
			Node.delete(payloadNode);
			Node.delete(httpBodyNode);
			Node.delete(methodNode);
		}
		return soapRequestNode;
	}

	private static final String systemUserDNFragment = "cn=SYSTEM,cn=organizational users,";
	private int getMappingInfo(final String key) {
		boolean deleteResponse = true;
		int requestNode = 0;
		int responseNode = 0;
		String error = null;
		try {			
			requestNode = getConnector().createSOAPMethod(systemUserDNFragment+organizationDN,organizationDN,
					"http://schemas.cordys.com/1.0/xmlstore", "GetXMLObject");
			Node.createTextElement("key", key, requestNode);
			if (logger.isDebugEnabled()) {
				logger.debug("SOAP Request to get xml template from xmlstore : \n"
						+ Node.writeToString(
								Node.getParent(Node.getParent(requestNode)),
								true));
			}
			responseNode = getConnector().sendAndWait(
					Node.getParent(Node.getParent(requestNode)));
			if (Node.getFirstElement(XPath.getFirstMatch(
					".//xmlst:GetXMLObjectResponse", oMeta, responseNode)) == 0) {
				error = "Client Error : Resource " + resourceURI
						+ " not available";
				throw new RESTGatewayException(
						RESTGatewayMessages.REST_RESOURCE_NOT_FOUND, error);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Response recieved from xmlstore : \n"
						+ Node.writeToString(responseNode, true));
			}
			deleteResponse = false;
		} catch (DirectoryException e) {
			logger.error(e, RESTGatewayMessages.REST_RESOURCE_NOT_FOUND, error);
			soapTransaction.raiseSOAPFault(Fault.Codes.SERVER, 500,
					RESTGatewayMessages.REST_RESOURCE_NOT_FOUND,
					new String[] { error }, e);
		} catch (TimeoutException e) {
			logger.error(e, RESTGatewayMessages.REST_RESOURCE_NOT_FOUND, error);
			soapTransaction.raiseSOAPFault(Fault.Codes.SERVER, 500,
					RESTGatewayMessages.REST_RESOURCE_NOT_FOUND,
					new String[] { error }, e);
		} catch (ExceptionGroup e) {
			logger.error(e, RESTGatewayMessages.REST_RESOURCE_NOT_FOUND, error);
			soapTransaction.raiseSOAPFault(Fault.Codes.SERVER, 500,
					RESTGatewayMessages.REST_RESOURCE_NOT_FOUND,
					new String[] { error }, e);
		} catch (RESTGatewayException e) {
			logger.error(e, RESTGatewayMessages.REST_RESOURCE_NOT_FOUND, error);
			soapTransaction.raiseSOAPFault(Fault.Codes.SERVER, 500,
					RESTGatewayMessages.REST_RESOURCE_NOT_FOUND,
					new String[] { error }, e);
		} finally {
			Node.delete(Node.getRoot(requestNode));
			if (deleteResponse) {
				Node.delete(Node.getRoot(responseNode));
			}
		}
		return responseNode;
	}

	private void setPrefixesToPayload(final int prefixesNode) {
		int[] namespacePrefixNodes = XPath.getMatchingNodes(
				".//NamespacePrefix", oMeta, prefixesNode);
		for (int namespacePrefixNode : namespacePrefixNodes) {
			String namespacePrefix = Node
					.getDataWithDefault(XPath.getFirstMatch(".//Prefix", oMeta,
							namespacePrefixNode), "");
			String namespaceDefined = Node.getDataWithDefault(XPath
					.getFirstMatch(".//Namespace", oMeta, namespacePrefixNode),
					"");
			oMeta.addNamespaceBinding(namespacePrefix, namespaceDefined);
		}
	}

	private String transformToString(final int inputXML, final int inputXSLT)
			throws RESTGatewayException {
		XSLT xslt = XSLT.parseFromString(Node.writeToString(inputXSLT, false));
		String response = xslt.xslTransformToString(inputXML);
		if (logger.isDebugEnabled()) {
			logger.debug("XSLT Transformation Response : " + response);
		}
		if (Util.isEmpty(response)) {
			String error = "Client Error : Error while transforming request "
					+ resourceURI;
			throw new RESTGatewayException(
					RESTGatewayMessages.REST_GATEWAY_BAD_REQUEST, error);
		}
		return response;
	}

	private int transformToXML(final int inputXML, final int inputXSLT)
			throws RESTGatewayException {
		XSLT xslt = XSLT.parseFromString(Node.writeToString(inputXSLT, false));
		int response = xslt.xslTransform(inputXML);
		if (logger.isDebugEnabled()) {
			logger.debug("XSLT Transformation Response : "
					+ Node.writeToString(response, true));
		}
		if (response == 0) {
			String error = "Client Error : Error while transforming request "
					+ resourceURI;
			throw new RESTGatewayException(
					RESTGatewayMessages.REST_GATEWAY_BAD_REQUEST, error);
		}
		return response;
	}

	public String createResponse(final SOAPTransaction soapTransaction,
			final byte[] responseBytes) {
		long startTime = counters.getStartTime();
		String response = null;
		int soapResponseNode = 0;
		int restResponseNode = 0;
		String error = "Server Error : Error while transforming response for resource "
				+ resourceURI;
		if (responseBytes == null) {
			response = "";
		} else {
			response = new String(responseBytes);
			try {
				soapResponseNode = DocumentPool.loadXML(responseBytes);
				if (XPath.getFirstMatch("..//soap:Fault", oMeta,
						soapResponseNode) == 0) {
					response = generateRESTResponse(Node.unlink(Node
							.getFirstElement(XPath.getFirstMatch(
									".//soap:Body", oMeta, soapResponseNode))));
				} else {
					response = FaultHandler.createRESTFault(soapResponseNode);
				}

			} catch (UnsupportedEncodingException e) {
				logger.error(e,
						RESTGatewayMessages.REST_GATEWAY_INTERNAL_SERVER_ERROR);
				soapTransaction.raiseSOAPFault(Fault.Codes.SERVER, 500,
						RESTGatewayMessages.REST_GATEWAY_INTERNAL_SERVER_ERROR,
						new String[] { error }, e);
			} catch (XMLException e) {
				logger.error(e,
						RESTGatewayMessages.REST_GATEWAY_INTERNAL_SERVER_ERROR);
				soapTransaction.raiseSOAPFault(Fault.Codes.SERVER, 500,
						RESTGatewayMessages.REST_GATEWAY_INTERNAL_SERVER_ERROR,
						new String[] { error }, e);
			} finally {
				Node.delete(soapResponseNode);
				Node.delete(restResponseNode);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug(RESTGatewayMessages.REST_GATEWAY_RESPONSE,
					resourceURI, response);
		}
		counters.finishResponseCreation(startTime);
		return response;
	}

	private String generateRESTResponse(final int soapResponseNode)
			throws XMLException, UnsupportedEncodingException {
		String response = null;
		int methodNode = 0;
		if (soapResponseNode != 0) {
			try {
				methodNode = DocumentPool.loadXML(methodXML);
				int responseXSLTNode = XPath.getFirstMatch(
						".//WSOperation[@URIPattern='" + uriPattern
								+ "']/Output/Filter[Type=" + "'xslt'"
								+ "]/Value/stylesheet", oMeta, methodNode);
				if (logger.isDebugEnabled()) {
					logger.debug("Output XSLT Node from xml template : \n"
							+ Node.writeToString(responseXSLTNode, true));
				}
				response = transformToString(soapResponseNode, responseXSLTNode);
			} catch (RESTGatewayException e) {
				String error = "Server Error : Error while transforming response for request "
						+ resourceURI;
				logger.error(new RESTGatewayException(
						RESTGatewayMessages.REST_GATEWAY_INTERNAL_SERVER_ERROR,
						error),
						RESTGatewayMessages.REST_GATEWAY_INTERNAL_SERVER_ERROR,
						error);
				soapTransaction.raiseSOAPFault(Fault.Codes.SERVER, 500,
						RESTGatewayMessages.REST_GATEWAY_INTERNAL_SERVER_ERROR,
						new String[] { error }, e);
			} finally {
				Node.delete(methodNode);
				Node.delete(soapResponseNode);
			}
		}
		return response;
	}

	@SuppressWarnings("PMD")
	// The code inside this method is guaranteed to return a single instance
	private static Connector getConnector() throws ExceptionGroup,
			DirectoryException {
		if (connector == null) {
			connector = Connector.getInstance("RESTGateway");
			if (!connector.isOpen()) {
				connector.open();
			}
		}
		return connector;
	}

	static void initJMXSettings(IManagedComponent managedComponent) {
		try {
			Connector connector = getConnector();
			connector.createManagedComponent(managedComponent,
					"RESTGatewayConnector", "RESTGatewayConnector");
		} catch (ExceptionGroup e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e,
						RESTGatewayMessages.FAILED_TO_INITIALIZE_JMXSettings);
			}
		} catch (DirectoryException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e,
						RESTGatewayMessages.FAILED_TO_INITIALIZE_JMXSettings);
			}
		}

	}
}
