package com.cordys.web.rest;

import java.io.UnsupportedEncodingException;

import com.eibus.xml.nom.XMLException;
import com.eibus.xml.nom.internal.NOMDocumentPool;

public class DocumentPool {

	private static NOMDocumentPool documentPool = NOMDocumentPool.getInstance();

	public static int loadXML(byte[] bytes) throws XMLException {
		return documentPool.load(bytes, bytes.length);
	}

	public static int loadXML(String xml) throws XMLException,
			UnsupportedEncodingException {
		return documentPool.parseString(xml);
	}
}
