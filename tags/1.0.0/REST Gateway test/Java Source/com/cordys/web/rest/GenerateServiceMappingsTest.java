package com.cordys.web.rest;

import static com.cordys.web.rest.RESTMapping.generateAndGetTheServiceRESTMapping;
import static com.cordys.web.rest.TestUtils.*;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cordys.cpc.bsf.busobject.BSF;
import com.eibus.connector.nom.Connector;
import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPath;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Connector.class, BSF.class })
public class GenerateServiceMappingsTest {
	@Mock
	private Connector connector;

	@Before
	public void prepareMocks() throws Exception {
		mockStatic(Connector.class);
		mockStatic(BSF.class);
		when(Connector.getInstance(anyString())).thenReturn(connector);
		when(BSF.getOrganization()).thenReturn(
				"o=org1,cn=cordys,cn=defaultInst,o=company.com");
		when(BSF.getUser())
				.thenReturn(
						"cn=user1,cn=organizational users,o=org1,cn=cordys,cn=defaultInst,o=company.com");

		int getCollectionMethod = createXMLStoreMethod("GetCollection");
		when(
				connector.createSOAPMethod(anyString(), anyString(),
						anyString(), eq("GetCollection"))).thenReturn(
				getCollectionMethod);
		when(connector.sendAndWait(Node.getRoot(getCollectionMethod)))
				.thenReturn(
						createXMLStoreResponse("GetCollectionResponse",
								"orders.xml"));

		int mappingMethod = createSOAPMethod(
				"http://schemas.cordys.com/RESTMapping",
				"generateAndGetTheServiceRESTMapping");
		when(
				connector.createSOAPMethod(anyString(), anyString(),
						anyString(), eq("generateAndGetTheServiceRESTMapping")))
				.thenReturn(mappingMethod);
		when(connector.sendAndWait(Node.getRoot(mappingMethod))).thenAnswer(
				new ExecuteRESTMapping());

		int getXMLObjectMethod = createXMLStoreMethod("GetXMLObject");
		when(
				connector.createSOAPMethod(anyString(), anyString(),
						anyString(), eq("GetXMLObject"))).thenReturn(
				getXMLObjectMethod);
		when(connector.sendAndWait(Node.getRoot(getXMLObjectMethod)))
				.thenReturn(
						createXMLStoreResponse("GetXMLObjectResponse",
								"servicemappings.xml"));
	}

	@After
	public void verifyInteractions() throws Exception {
		verify(connector, atLeastOnce()).isOpen();
		verify(connector, atLeastOnce()).open();
	}

	@Test
	public void no_updates_required() throws Exception {
		assertTrue(GenerateServiceMappings.generateServiceMappings());
	}

	private class ExecuteRESTMapping implements Answer<Integer> {
		@Override
		public Integer answer(InvocationOnMock invocation) throws Throwable {
			int soapEnvelope = (Integer) invocation.getArguments()[0];
			int namespace = XPath.getFirstMatch(".//stringParam1", null,
					soapEnvelope);
			int secondTuple = generateAndGetTheServiceRESTMapping(Node
					.getData(namespace));

			// <generateAndGetTheServiceRESTMappingResponse
			// xmlns="http://schemas.cordys.com/RESTMapping">
			// <tuple xmlns="http://schemas.cordys.com/RESTMapping">
			// <old>
			// <generateAndGetTheServiceRESTMapping>
			// <generateAndGetTheServiceRESTMapping>
			// <tuple xmlns="http://schemas.cordys.com/1.0/xmlstore">

			String methodName = "generateAndGetTheServiceRESTMapping";
			int responseNode = createSOAPMethod(
					"http://schemas.cordys.com/RESTMapping", methodName
							+ "Response");
			int firstTuple = Node.createElementNS("tuple", null, null,
					"http://schemas.cordys.com/RESTMapping", responseNode);
			int firstOld = Node.createElementNS("old", null, null,
					"http://schemas.cordys.com/RESTMapping", firstTuple);
			int firstMapping = Node.createElementNS(methodName, null, null,
					"http://schemas.cordys.com/RESTMapping", firstOld);
			int secondMapping = Node.createElementNS(methodName, null, null,
					"http://schemas.cordys.com/RESTMapping", firstMapping);
			Node.appendToChildren(secondTuple, secondMapping);
			return Node.getRoot(responseNode);
		}
	}
}
