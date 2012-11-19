package com.cordys.web.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.cordys.cpc.bsf.busobject.BSF;
import com.eibus.connector.nom.Connector;
import com.eibus.directory.soap.DirectoryException;
import com.eibus.exception.ExceptionGroup;
import com.eibus.exception.TimeoutException;
import com.eibus.util.logger.CordysLogger;
import com.eibus.xml.nom.Document;
import com.eibus.xml.nom.Node;
import com.eibus.xml.nom.XMLException;
import com.eibus.xml.xpath.XPath;
import com.eibus.xml.xpath.XPathMetaInfo;

public class GenerateServiceMappings {

	private static Connector connector;
	private static final String pathForResourceXMLs = "/Cordys/servicemapping/resttosoap";
	private static XPathMetaInfo oMeta = new XPathMetaInfo();
	private static CordysLogger logger = CordysLogger
			.getCordysLogger(GenerateServiceMappings.class);

	public static boolean generateServiceMappings() throws DirectoryException,
			TimeoutException, ExceptionGroup, GenerateServiceMappingsException,
			XMLException {
		int resourcesXML = 0;
		boolean returnValue = true;
		try {
			resourcesXML = getResourceXMLs();
			Map<String, ArrayList<ServiceMapping>> serviceMappings = prepareServiceMappings(resourcesXML);
			createServiceMappings(serviceMappings);
		} finally {
			Node.delete(Node.getRoot(resourcesXML));
		}
		return returnValue;
	}

	private static int getResourceXMLs() throws DirectoryException,
			TimeoutException, ExceptionGroup, GenerateServiceMappingsException {
		int request = 0;
		int response = 0;
		boolean deleteResponse = true;
		try {
			Connector connector = getConnector();
			request = connector.createSOAPMethod(BSF.getUser(),
					BSF.getOrganization(),
					"http://schemas.cordys.com/1.0/xmlstore", "GetCollection");
			int folderElement = Node.createTextElement("folder",
					pathForResourceXMLs, request);
			Node.setAttribute(folderElement, "recursive", "false");
			Node.setAttribute(folderElement, "detail", "true");
			Node.setAttribute(folderElement, "version", "");
			response = connector.sendAndWait(Node.getRoot(request));
			if (XPath.getFirstMatch(".//fault", oMeta, response) != 0) {
				String faultString = Node.getDataWithDefault(
						XPath.getFirstMatch(".//faultstring", oMeta, response),
						"");
				GenerateServiceMappingsException exception = new GenerateServiceMappingsException(
						RESTGatewayMessages.GENERATE_SERVICE_MAPPING_ERROR,
						faultString);
				logger.error(exception,
						RESTGatewayMessages.GENERATE_SERVICE_MAPPING_ERROR,
						faultString);
				throw exception;
			}
			deleteResponse = false;
		} finally {
			Node.delete(Node.getRoot(request));
			if (deleteResponse) {
				Node.delete(Node.getRoot(response));
			}
		}
		return response;
	}

	private static Map<String, ArrayList<ServiceMapping>> prepareServiceMappings(
			final int resourcesXML) {
		Map<String, ArrayList<ServiceMapping>> serviceMappings = new HashMap<String, ArrayList<ServiceMapping>>();
		int resourceNode = 0;
		for (int mappingNode = XPath.getFirstMatch(
				"Body/GetCollectionResponse/tuple", oMeta, resourcesXML); mappingNode != 0; mappingNode = Node
				.getNextElement(mappingNode)) {
			resourceNode = XPath.getFirstMatch(".//Resource", oMeta,
					mappingNode);
			if (resourceNode == 0) {
				continue;
			}
			String resourceName = Node.getData(XPath.getFirstMatch("Name",
					oMeta, resourceNode));
			int webserviceNode = 0;
			for (webserviceNode = Node.getFirstElement(XPath.getFirstMatch(
					"old/RESTMapping", oMeta, mappingNode)); webserviceNode != 0; webserviceNode = Node
					.getNextElement(webserviceNode)) {
				String httpMethod = Node.getLocalName(webserviceNode);
				for (int wsOperationNode = XPath.getFirstMatch("WSOperation",
						oMeta, webserviceNode); wsOperationNode != 0; wsOperationNode = Node
						.getNextElement(wsOperationNode)) {
					String uriPattern = Node.getAttribute(wsOperationNode,
							"URIPattern");
					String wsOperation = Node.getData(XPath.getFirstMatch(
							"Name", oMeta, wsOperationNode));
					String wsNamespace = Node.getData(XPath.getFirstMatch(
							"Namespace", oMeta, wsOperationNode));

					ServiceMapping mappingElement = new ServiceMapping();
					mappingElement.setHttpMethodType(httpMethod);
					mappingElement.setResourceName(resourceName);
					mappingElement.setUriPattern(uriPattern);
					mappingElement.setWebserviceName(wsOperation);

					if (!serviceMappings.containsKey(wsNamespace)) {
						ArrayList<ServiceMapping> servicemappingElements = new ArrayList<ServiceMapping>();
						servicemappingElements.add(mappingElement);
						serviceMappings
								.put(wsNamespace, servicemappingElements);
					} else {
						ArrayList<ServiceMapping> servicemappingList = serviceMappings
								.get(wsNamespace);
						servicemappingList.add(mappingElement);
						serviceMappings.put(wsNamespace, servicemappingList);
					}
				}
			}
		}
		return serviceMappings;
	}

	private static void createServiceMappings(
			Map<String, ArrayList<ServiceMapping>> serviceMappings)
			throws XMLException, DirectoryException, TimeoutException,
			ExceptionGroup, GenerateServiceMappingsException {
		String serviceMappingStr = "";
		int serviceRestMapping = 0;
		int serviceMappingsElement = 0;
		int serviceMappingsNode = 0;
		int tupleNode = 0;
		for (Entry<String, ArrayList<ServiceMapping>> entry : serviceMappings
				.entrySet()) {
			try {
				String namespace = entry.getKey();
				serviceRestMapping = createServiceMappingStructure(namespace);
				tupleNode = XPath
						.getFirstMatch(
								".//generateAndGetTheServiceRESTMappingResponse/tuple/old/generateAndGetTheServiceRESTMapping/generateAndGetTheServiceRESTMapping/tuple",
								oMeta, serviceRestMapping);
				if (tupleNode == 0) {
					GenerateServiceMappingsException exception = new GenerateServiceMappingsException(
							RESTGatewayMessages.GENERATE_SERVICE_MAPPING_ERROR_MESSAGE);
					logger.error(
							exception,
							RESTGatewayMessages.GENERATE_SERVICE_MAPPING_ERROR_MESSAGE);
					throw exception;
				}
				String pathValue = Node.getAttribute(tupleNode, "key");
				String fileName = Node.getAttribute(tupleNode, "name");
				String lastModified = Node.getAttribute(tupleNode,
						"lastModified");
				if (pathValue.isEmpty() || lastModified.isEmpty()
						|| fileName.isEmpty()) {
					GenerateServiceMappingsException exception = new GenerateServiceMappingsException(
							RESTGatewayMessages.GENERATE_SERVICE_MAPPING_ERROR_MESSAGE);
					logger.error(
							exception,
							RESTGatewayMessages.GENERATE_SERVICE_MAPPING_ERROR_MESSAGE);
					throw exception;
				}
				serviceMappingsElement = Node.unlink(XPath.getFirstMatch(
						".//old/ServiceMappings", oMeta, tupleNode));
				if (Node.getNumChildElements(serviceMappingsElement) > 0) {
					continue;
				}
				for (ServiceMapping serviceMappingObject : entry.getValue()) {
					serviceMappingStr += "<ServiceMapping><WSOperation>"
							+ serviceMappingObject.getWebserviceName()
							+ "</WSOperation><URIPattern>"
							+ serviceMappingObject.getUriPattern()
							+ "</URIPattern><HttpMethod>"
							+ serviceMappingObject.getHttpMethodType()
							+ "</HttpMethod><Resource>"
							+ serviceMappingObject.getResourceName()
							+ "</Resource></ServiceMapping>";
				}
				serviceMappingStr = "<mappings>" + serviceMappingStr
						+ "</mappings>";
				Document doc = Node.getDocument(tupleNode);
				serviceMappingsNode = doc.load(serviceMappingStr.getBytes());
				Node.appendToChildren(
						Node.getFirstChildElement(serviceMappingsNode), 0,
						serviceMappingsElement);
				int newNode = serviceMappingsElement;
				serviceMappingsElement = 0;
				int oldNode = Node.duplicate(newNode);
				updateXMLObject(fileName, pathValue, lastModified, newNode,
						oldNode);
			} finally {
				// tupleNode is a part of serviceRestMapping node so it does not
				// need to be deleted
				Node.delete(serviceRestMapping);
				Node.delete(serviceMappingsElement);
				Node.delete(serviceMappingsNode);
			}
		}
	}

	private static int createServiceMappingStructure(final String namespace)
			throws DirectoryException, TimeoutException, ExceptionGroup,
			GenerateServiceMappingsException {
		boolean deleteResponse = true;
		int request = 0;
		int response = 0;
		try {
			Connector connector = getConnector();
			request = connector.createSOAPMethod(BSF.getUser(),
					BSF.getOrganization(),
					"http://schemas.cordys.com/RESTMapping",
					"generateAndGetTheServiceRESTMapping");
			Node.createTextElement("stringParam1", namespace, request);
			response = connector.sendAndWait(Node.getRoot(request));
			if (XPath.getFirstMatch(".//fault", oMeta, response) != 0) {
				String faultString = Node.getDataWithDefault(
						XPath.getFirstMatch(".//faultstring", oMeta, response),
						"");
				GenerateServiceMappingsException exception = new GenerateServiceMappingsException(
						RESTGatewayMessages.GENERATE_SERVICE_MAPPING_ERROR,
						faultString);
				logger.error(exception,
						RESTGatewayMessages.GENERATE_SERVICE_MAPPING_ERROR,
						faultString);
				throw exception;
			}
			deleteResponse = false;
		} finally {
			Node.delete(Node.getRoot(request));
			if (deleteResponse) {
				Node.delete(Node.getRoot(response));
			}
		}
		return response;
	}

	private static void updateXMLObject(final String fileName,
			final String key, final String lastModified, int newNode,
			int oldNode) throws DirectoryException, TimeoutException,
			ExceptionGroup, GenerateServiceMappingsException {
		int request = 0;
		int oldElement = 0;
		int newElement = 0;
		int response = 0;
		try {
			Connector connector = getConnector();
			request = connector
					.createSOAPMethod(BSF.getUser(), BSF.getOrganization(),
							"http://schemas.cordys.com/1.0/xmlstore",
							"UpdateXMLObject");
			int tupleElement = Node.createElement("tuple", request);
			Node.setAttribute(tupleElement, "key", key);
			Node.setAttribute(tupleElement, "name", fileName);
			Node.setAttribute(tupleElement, "lastModified", lastModified);
			oldElement = Node.createElement("old", tupleElement);
			newElement = Node.createElement("new", tupleElement);
			Node.appendToChildren(oldNode, oldElement);
			oldNode = 0;
			Node.appendToChildren(newNode, newElement);
			newNode = 0;
			response = connector.sendAndWait(Node.getRoot(request));
			if (XPath.getFirstMatch(".//fault", oMeta, response) != 0) {
				String faultString = Node.getDataWithDefault(
						XPath.getFirstMatch(".//faultstring", oMeta, response),
						"");
				GenerateServiceMappingsException exception = new GenerateServiceMappingsException(
						RESTGatewayMessages.GENERATE_SERVICE_MAPPING_ERROR,
						faultString);
				logger.error(exception,
						RESTGatewayMessages.GENERATE_SERVICE_MAPPING_ERROR,
						faultString);
				throw exception;
			}
		} finally {
			Node.delete(Node.getRoot(request));
			Node.delete(Node.getRoot(response));
			Node.delete(oldNode);
			Node.delete(newNode);
		}
	}

	@SuppressWarnings("PMD")
	// The code inside this method is guaranteed to return a single instance
	private static Connector getConnector() throws ExceptionGroup,
			DirectoryException {
		if (connector == null) {
			connector = Connector.getInstance("GenerateServiceMappings");
			if (!connector.isOpen()) {
				connector.open();
			}
		}
		return connector;
	}
}
