package com.cordys.web.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.cordys.xml.dom.NOMNode;
import com.cordys.xml.dom.internal.DocumentBuilderFactoryImpl;
import com.eibus.xml.nom.Node;
import com.eibus.xml.nom.XMLException;

public final class TestUtils {
	private static DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = new DocumentBuilderFactoryImpl();
	private static com.eibus.xml.nom.Document doc = new com.eibus.xml.nom.Document();
	static {
		DOCUMENT_BUILDER_FACTORY.setNamespaceAware(true);
		DOCUMENT_BUILDER_FACTORY.setIgnoringElementContentWhitespace(true);
		DOCUMENT_BUILDER_FACTORY.setIgnoringComments(true);
	}

	private TestUtils() {
		// only static access
	}

	/**
	 * Reads the given resource and parses it using DOM-over-NOM to XML. Caller
	 * is responsible for memory cleanup.
	 * 
	 * @param resource
	 *            The resource to parse.
	 * @return A NOM Node referencing to the XML.
	 * @throws IOException
	 *             If the resource cannot be read.
	 * @throws SAXException
	 *             If the resource contains no or invalid XML.
	 * @throws XMLException
	 *             If converting to NOM fails.
	 */
	public static int parse(final URL resource) throws IOException,
			SAXException, XMLException {
		try {
			InputStream is = resource.openStream();
			DocumentBuilder docBuilder = DOCUMENT_BUILDER_FACTORY
					.newDocumentBuilder();
			Document doc = docBuilder.parse(is, resource.toString());
			NOMNode root = (NOMNode) doc.getDocumentElement();
			return Node.duplicate(root.getNOMNode());
		} catch (ParserConfigurationException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Reads the given resource and return it as byte array.
	 * 
	 * @param resource
	 *            The resource to read.
	 * @return a byte array with the content of the resource.
	 * @throws IOException
	 *             If the resource cannot be read.
	 */
	public static byte[] read(final URL resource) throws IOException {
		InputStream in = resource.openStream();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		for (int n = in.read(buffer); n >= 0; n = in.read(buffer)) {
			out.write(buffer, 0, n);
		}
		return out.toByteArray();
	}

	public static int createSOAPMethod(String namespace, String methodName)
			throws XMLException, UnsupportedEncodingException {
		int envelope = doc
				.parseString("<SOAP:Envelope xmlns:SOAP='http://schemas.xmlsoap.org/soap/envelope/'><SOAP:Body>"
						+ "<"
						+ methodName
						+ " xmlns='"
						+ namespace
						+ "'/>"
						+ "</SOAP:Body></SOAP:Envelope>");
		return Node.getFirstChildElement(Node.getFirstChildElement(envelope));
	}

	public static int createXMLStoreMethod(String methodName)
			throws XMLException, UnsupportedEncodingException {
		return createSOAPMethod("http://schemas.cordys.com/1.0/xmlstore",
				methodName);
	}

	public static int createXMLStoreResponse(String methodReponseName,
			String resource) throws IOException, SAXException, XMLException {
		int method = createXMLStoreMethod(methodReponseName);
		int tuple = parse(TestUtils.class.getResource(resource));
		Node.appendToChildren(tuple, method);
		return Node.getRoot(method);
	}
}
