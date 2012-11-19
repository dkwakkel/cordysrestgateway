package com.cordys.web.rest;

import com.eibus.xml.nom.Node;
import com.eibus.xml.nom.XMLException;
import com.eibus.xml.xpath.XPath;

public class FaultHandler {

	public static String createRESTFault(int soapFaultNode) throws XMLException {
		int faultNode = 0;
		try {
			byte[] restFault = "<RESTFault/>".getBytes();
			faultNode = DocumentPool.loadXML(restFault);
			Node.appendToChildren(
					XPath.getFirstMatch(".//faultcode", null, soapFaultNode),
					faultNode);
			Node.appendToChildren(
					XPath.getFirstMatch(".//faultstring", null, soapFaultNode),
					faultNode);
			Node.appendToChildren(
					XPath.getFirstMatch(".//detail", null, soapFaultNode),
					faultNode);
			Node.removeAttributesRecursive(faultNode, "xmlns:SOAP", "",
					"", "");
			return Node.writeToString(faultNode, true);
		} finally {
			Node.delete(faultNode);
		}
	}
}
