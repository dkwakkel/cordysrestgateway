package com.cordys.web.rest;

import static com.cordys.web.rest.TestUtils.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.XMLUnit;
import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.eibus.connector.nom.Connector;
import com.eibus.localization.IStringResource;
import com.eibus.web.gateway.SOAPTransaction;
import com.eibus.web.gateway.interceptor.api.IInterceptorContext;
import com.eibus.web.isapi.ExtensionControlBlock;
import com.eibus.web.isapi.Request;
import com.eibus.web.isapi.Response;
import com.eibus.web.soap.GatewayException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Connector.class)
public class RESTGatewayTest {
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	@Mock
	private Connector connector;
	@Mock
	private SOAPTransaction soapTransaction;
	@Mock
	private ExtensionControlBlock ecb;
	@Mock
	private Request request;
	@Mock
	private Response response;
	@Mock
	private IInterceptorContext context;

	private static Connector s_connector;
	private static RESTGateway gateway;

	@Before
	public void prepareMocks() throws Exception {
		if (gateway == null) {
			gateway = new RESTGateway();// This is a JUnit test case so
										// synchronization is not required
		}
		mockStatic(Connector.class);
		// To ensure there is only one mock instance of connector
		if (s_connector == null) {
			s_connector = connector;
		}
		connector = s_connector;
		when(Connector.getInstance(anyString())).thenReturn(connector);
		when(connector.createSOAPMethod(anyString(), anyString(), anyString()))
				.thenReturn(createXMLStoreMethod("GetXMLObject"));
		when(connector.sendAndWait(anyInt())).thenReturn(
				createXMLStoreResponse("GetXMLObjectResponse", "orders.xml"));
		when(soapTransaction.getExtensionControlBlock()).thenReturn(ecb);
		whenRaiseSOAPFaultDoThrowException(soapTransaction);
		when(ecb.getRequest()).thenReturn(request);
		when(ecb.getResponse()).thenReturn(response);
		when(request.binaryRead()).thenReturn(new byte[0]);
		when(soapTransaction.getContext()).thenReturn(context);
		XMLUnit.setIgnoreWhitespace(true);
	}

	private static void whenRaiseSOAPFaultDoThrowException(
			final SOAPTransaction st) {
		RuntimeException e = new RuntimeException("Stopper");
		Class<IStringResource> iSR = IStringResource.class;
		Class<Object[]> oa = Object[].class;
		doThrow(e).when(st).raiseSOAPFault(any(GatewayException.class));
		doThrow(e).when(st).raiseSOAPFault(any(QName.class), anyInt(),
				any(iSR), any(oa));
		doThrow(e).when(st).raiseSOAPFault(any(QName.class), anyInt(),
				any(iSR), any(oa), anyInt());
		doThrow(e).when(st).raiseSOAPFault(any(QName.class), anyInt(),
				any(iSR), any(oa), any(Throwable.class));
		doThrow(e).when(st).raiseSOAPFault(any(QName.class), anyInt(),
				any(iSR), any(oa), any(iSR), any(oa));
		doThrow(e).when(st).raiseSOAPFault(any(QName.class), anyInt(),
				any(iSR), any(oa), any(Throwable.class), anyInt());
		doThrow(e).when(st).raiseSOAPFault(any(QName.class), anyInt(),
				any(iSR), any(oa), any(Throwable.class), any(iSR), any(oa),
				anyInt());
	}

	private static String newRestUrl(final String resourceURI) {
		return "/cordys/restful/" + resourceURI + "/"
				+ RESTGateway.class.getCanonicalName() + ".wcp";
	}

	private static String newEnvelope(final String methodxml) {
		return "<SOAP:Envelope xmlns:SOAP='http://schemas.xmlsoap.org/soap/envelope/'><SOAP:Body>"
				+ methodxml + "</SOAP:Body></SOAP:Envelope>";
	}

	@Test
	public void get_single_order_rest_to_soap() throws Exception {
		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn(newRestUrl("orders/1"));

		String actual = new String(gateway.getRequestXML(soapTransaction));

		String expected = newEnvelope("<GetOrdersObject xmlns='testns'><OrderID>1</OrderID></GetOrdersObject>");
		assertXMLEqual(expected, actual);
	}

	@Test
	public void get_single_order_soap_to_rest() throws Exception {
		String soapResponse = newEnvelope("<GetOrdersObjectResponse xmlns='testns'>"
				+ "<tuple><old><Order><Customer>MyCompany Inc.</Customer></Order></old></tuple>"
				+ "</GetOrdersObjectResponse>");

		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn(newRestUrl("orders/1"));
		RestTransaction restTransaction = new RestTransaction(soapTransaction,
				PerformanceCounters.Factory.getPerformanceCounters(null), null);
		restTransaction.createSOAPRequest();
		when(context.getAttribute(anyString())).thenReturn(restTransaction);
		gateway.setResponseXML(soapTransaction, soapResponse.getBytes("UTF-8"));

		ArgumentCaptor<String> actual = ArgumentCaptor.forClass(String.class);
		verify(response).write(actual.capture());
		String expected = "<Order xmlns='testns'><Customer>MyCompany Inc.</Customer></Order>";
		assertXMLEqual(expected, actual.getValue());
	}

	@Test
	public void get_multiple_orders_rest_to_soap() throws Exception {
		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn(newRestUrl("orders/1/2"));

		String actual = new String(gateway.getRequestXML(soapTransaction));

		String expected = newEnvelope("<GetOrdersObjects xmlns='testns'><FromID>1</FromID><ToID>2</ToID></GetOrdersObjects>");
		assertXMLEqual(expected, actual);
	}

	@Test
	public void get_multiple_orders_soap_to_rest() throws Exception {
		String soapResponse = newEnvelope("<GetOrdersObjectResponse xmlns='testns'>"
				+ "<tuple><old><Order><Customer>MyFirstCompany Inc.</Customer></Order></old></tuple>"
				+ "<tuple><old><Order><Customer>MySecondCompany Inc.</Customer></Order></old></tuple>"
				+ "</GetOrdersObjectResponse>");

		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn(newRestUrl("orders/1/2"));
		RestTransaction restTransaction = new RestTransaction(soapTransaction,
				PerformanceCounters.Factory.getPerformanceCounters(null), null);
		restTransaction.createSOAPRequest();
		when(context.getAttribute(anyString())).thenReturn(restTransaction);
		gateway.setResponseXML(soapTransaction, soapResponse.getBytes("UTF-8"));

		ArgumentCaptor<String> actual = ArgumentCaptor.forClass(String.class);
		verify(response).write(actual.capture());
		String expected = "<Orders xmlns='testns'>"
				+ "<Order><Customer>MyFirstCompany Inc.</Customer></Order>"
				+ "<Order><Customer>MySecondCompany Inc.</Customer></Order>"
				+ "</Orders>";
		assertXMLEqual(expected, actual.getValue());
	}

	@Test
	public void delete_single_order_rest_to_soap() throws Exception {
		when(request.getMethod()).thenReturn("DELETE");
		when(request.getPathInfo()).thenReturn(newRestUrl("orders/1"));

		String actual = new String(gateway.getRequestXML(soapTransaction));

		String expected = newEnvelope("<UpdateOrders xmlns='testns'>"
				+ "<tuple><old><Orders><OrderID>1</OrderID></Orders></old></tuple>"
				+ "</UpdateOrders>");
		assertXMLEqual(expected, actual);
	}

	@Test
	public void put_single_order_rest_to_soap() throws Exception {
		String inputXML = "<RestOrders><tuple><old>"
				+ "<OrderNumber>1</OrderNumber>" + "</old><new>"
				+ "<Customer>MyCompany Inc.</Customer>"
				+ "</new></tuple></RestOrders>";
		when(request.getMethod()).thenReturn("PUT");
		when(request.getPathInfo()).thenReturn(newRestUrl("orders"));
		when(request.binaryRead()).thenReturn(inputXML.getBytes("UTF-8"));

		String actual = new String(gateway.getRequestXML(soapTransaction));

		String expected = newEnvelope("<UpdateOrders xmlns='testns'><tuple><old>"
				+ "<Orders><OrderID>1</OrderID></Orders>"
				+ "</old><new>"
				+ "<Orders><OrderID>1</OrderID><Customer>MyCompany Inc.</Customer></Orders>"
				+ "</new></tuple></UpdateOrders>");
		assertXMLEqual(expected, actual);
	}

	@Test
	public void post_single_order_rest_to_soap() throws Exception {
		String inputXML = "<RestOrders><OrderNumber>1</OrderNumber><Customer>MyCompany Inc.</Customer></RestOrders>";
		when(request.getMethod()).thenReturn("POST");
		when(request.getPathInfo()).thenReturn(newRestUrl("orders"));
		when(request.binaryRead()).thenReturn(inputXML.getBytes("UTF-8"));

		String actual = new String(gateway.getRequestXML(soapTransaction));

		String expected = newEnvelope("<UpdateOrders xmlns='testns'><tuple><new><Orders>"
				+ "<OrderID>1</OrderID>"
				+ "<Customer>MyCompany Inc.</Customer>"
				+ "</Orders></new></tuple></UpdateOrders>");
		assertXMLEqual(expected, actual);
	}

	@Test
	public void target_web_service_raised_a_soap_fault() throws Exception {
		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn(newRestUrl("orders/1"));
		RestTransaction restTransaction = new RestTransaction(soapTransaction,
				PerformanceCounters.Factory.getPerformanceCounters(null), null);
		restTransaction.createSOAPRequest();
		when(context.getAttribute(anyString())).thenReturn(restTransaction);
		gateway.setResponseXML(soapTransaction,
				read(getClass().getResource("soapfault.xml")));

		ArgumentCaptor<String> actual = ArgumentCaptor.forClass(String.class);
		verify(response).write(actual.capture());
		String expected = new String(read(getClass().getResource(
				"restfault.xml")), "UTF-8");
		assertXMLEqual(expected, actual.getValue());
	}

	@Test
	public void resource_name_empty() throws Exception {
		expectedException.expectMessage(new IsEqual<String>("Stopper"));
		when(request.getMethod()).thenReturn("GET");
		when(request.getPathInfo()).thenReturn(newRestUrl(""));

		gateway.getRequestXML(soapTransaction);
	}
}
