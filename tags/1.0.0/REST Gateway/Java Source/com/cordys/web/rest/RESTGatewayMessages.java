package com.cordys.web.rest;

import com.eibus.localization.message.Message;
import com.eibus.localization.message.MessageSet;

public class RESTGatewayMessages {

	public static final MessageSet MESSAGE_SET = MessageSet
			.getMessageSet("Cordys.ReSTGateway.Messages");

	/** Bad Request. Error Message : {0} */
	public static final Message REST_GATEWAY_BAD_REQUEST = MESSAGE_SET
			.getMessage("RestGatewayBadRequest");

	/** Resource Not Found. Error Message : {0} */
	public static final Message REST_RESOURCE_NOT_FOUND = MESSAGE_SET
			.getMessage("RestResourceNotFound");

	/** Internal Server Error. Error Message : {0} */
	public static final Message REST_GATEWAY_INTERNAL_SERVER_ERROR = MESSAGE_SET
			.getMessage("RestGatewayInternalServerError");

	/** REST Request received is : {0} */
	public static final Message REST_GATEWAY_REQUEST = MESSAGE_SET
			.getMessage("RestGatewayRequest");

	/** SOAP Request created for REST URI {0} is : {1} */
	public static final Message REST_GATEWAY_SOAPREQUEST = MESSAGE_SET
			.getMessage("RestGatewaySOAPRequest");

	/** SOAP Response for REST URI {0} is : {1} */
	public static final Message REST_GATEWAY_SOAPRESPONSE = MESSAGE_SET
			.getMessage("RestGatewaySOAPResponse");

	/** Response for REST URI {0} is : {1} */
	public static final Message REST_GATEWAY_RESPONSE = MESSAGE_SET
			.getMessage("RestGatewayResponse");

	/** REST Mapping for namespace {0} is {1} */
	public static final Message REST_MAPPING = MESSAGE_SET
			.getMessage("RestMapping");

	/** Generate Service Mapping Failed with error {0} */
	public static final Message GENERATE_SERVICE_MAPPING_ERROR = MESSAGE_SET
			.getMessage("GenerateServiceMappingError");

	/**
	 * Can not proceed further. There is an internal problem with
	 * generateAndGetTheServiceRESTMapping webservice.
	 */
	public static final Message GENERATE_SERVICE_MAPPING_ERROR_MESSAGE = MESSAGE_SET
			.getMessage("GenerateServiceMappingErrorMessage");

	/** RESTRequestProcessingCounter */
	public static final Message REST_REQUEST_PROCESSING_COUNTER = MESSAGE_SET
			.getMessage("RESTRequestProcessingCounter");

	/** RESTResponseCreationCounter */
	public static final Message REST_RESPONSE_CREATION_COUNTER = MESSAGE_SET
			.getMessage("RESTResponseCreationCounter");

	/** Failed to initialize JMX settings for RestGatewayConnector */
	public static final Message FAILED_TO_INITIALIZE_JMXSettings = MESSAGE_SET
			.getMessage("FailedToInitializeJMXSettings");

}