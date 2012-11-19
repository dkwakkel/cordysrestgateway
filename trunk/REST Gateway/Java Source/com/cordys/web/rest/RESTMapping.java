package com.cordys.web.rest;

import com.cordys.cpc.bsf.busobject.BSF;
import com.eibus.connector.nom.Connector;
import com.eibus.directory.soap.DirectoryException;
import com.eibus.exception.ExceptionGroup;
import com.eibus.exception.TimeoutException;
import com.eibus.util.logger.CordysLogger;
import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPath;
import com.eibus.xml.xpath.XPathMetaInfo;

public class RESTMapping {
	private static final String XML_NAME_SPACE = "http://schemas.cordys.com/1.0/xmlstore";
	private static final String GET_XMLOBJECT = "GetXMLObject";
	private static final String XMLSTOREPATH = "/Cordys/servicemapping/soaptorest/";
	private static final String SERVICE_REST_MAPPING_NAME = "servicemappings.xml";
	private static final String ORGVERSION = "organization";
	private static Connector connector;
	private static XPathMetaInfo oMeta = new XPathMetaInfo();
	private static CordysLogger logger = CordysLogger
			.getCordysLogger(RESTMapping.class);

	private static String generateKey(final String nameSpace) {
		long hashCode = generateUniqueHash(nameSpace);
		StringBuilder stb = new StringBuilder(XMLSTOREPATH);
		stb.append(hashCode);
		stb.append("/");
		stb.append(SERVICE_REST_MAPPING_NAME);
		return stb.toString();
	}

	/**
	 * Generates a hash based on the ELF Hashing algorithm. Definitely not
	 * unique!
	 * 
	 * @param source
	 *            The source from which the hash is taken
	 * @return The ELF Hash code
	 */
	private static long generateUniqueHash(final String source) {
		long hash = 0;
		long x = 0;

		for (int i = 0; i < source.length(); i++) {
			hash = (hash << 4) + source.charAt(i);

			if ((x = hash & 0xF0000000L) != 0) {
				hash ^= (x >> 24);
			}
			hash &= ~x;
		}

		return hash;
	}

	public static int generateAndGetTheServiceRESTMapping(final String nameSpace)
			throws TimeoutException, ExceptionGroup, DirectoryException {
		String key = generateKey(nameSpace);
		String userDn = BSF.getUser();
		String orgDn = BSF.getOrganization();
		int mappingResponse = 0;
		int response = 0;
		int updateRequest = 0;
		int updateResponse = 0;
		try {
			Connector connector = getConnector();
			mappingResponse = getMappingInfo(userDn, orgDn, XML_NAME_SPACE,
					GET_XMLOBJECT, key);
			response = Node.unlink(XPath.getFirstMatch(
					".//GetXMLObjectResponse/tuple", oMeta, mappingResponse));
			if (response == 0) {
				updateRequest = connector.createSOAPMethod(userDn, orgDn,
						"http://schemas.cordys.com/1.0/xmlstore",
						"UpdateXMLObject");
				int tupleElement = Node.createElement("tuple", updateRequest);
				Node.setAttribute(tupleElement, "key", key);
				Node.setAttribute(tupleElement, "name",
						SERVICE_REST_MAPPING_NAME);
				Node.setAttribute(tupleElement, "version", ORGVERSION);
				int newElement = Node.createElement("new", tupleElement);
				Node.setAttribute(
						Node.createElement("ServiceMappings", newElement),
						"xmlns", nameSpace);
				updateResponse = connector.sendAndWait(Node.getParent(Node
						.getParent(updateRequest)));

				Node.delete(Node.getRoot(mappingResponse));
				mappingResponse = 0;
				mappingResponse = getMappingInfo(userDn, orgDn, XML_NAME_SPACE,
						GET_XMLOBJECT, key);
				response = Node.unlink(XPath
						.getFirstMatch(".//GetXMLObjectResponse/tuple", oMeta,
								mappingResponse));
			}
		} finally {
			Node.delete(Node.getRoot(mappingResponse));
			Node.delete(Node.getRoot(updateRequest));
			Node.delete(Node.getRoot(updateResponse));
		}
		if (logger.isDebugEnabled()) {
			logger.debug(RESTGatewayMessages.REST_MAPPING, nameSpace,
					Node.writeToString(response, true));
		}
		return response;
	}

	private static int getMappingInfo(final String userDN, final String orgDN,
			final String nameSpace, final String MethodName, final String key)
			throws ExceptionGroup, DirectoryException, TimeoutException {
		int request = 0;
		int responseXML = 0;
		try {
			Connector connector = getConnector();
			request = connector.createSOAPMethod(userDN, orgDN, nameSpace,
					MethodName);
			Node.createTextElement("key", key, request);
			responseXML = connector.sendAndWait(Node.getParent(Node
					.getParent(request)));
		} finally {
			Node.delete(Node.getRoot(request));
		}
		return responseXML;
	}

	@SuppressWarnings("PMD")
	// The code inside this method is guaranteed to return a single instance
	private static Connector getConnector() throws ExceptionGroup,
			DirectoryException {
		if (connector == null) {
			connector = Connector.getInstance("RESTMapping");
			if (!connector.isOpen()) {
				connector.open();
			}
		}
		return connector;
	}
}