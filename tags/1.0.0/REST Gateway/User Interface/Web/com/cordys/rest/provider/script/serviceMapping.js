var serviceMapping = 0;
var MAPPING_NAMESPACE_URI = "";

function ServiceMappings()
{
    this.serviceMappings = [];
}

ServiceMappings.prototype.addServiceMapping = function(serviceMapping)
{
    this.serviceMappings.push(serviceMapping);
};

ServiceMappings.prototype.removeServiceMapping = function(serviceMapping)
{
    this.serviceMappings.pop(serviceMapping);
};

ServiceMappings.prototype.getAllServiceMappings = function()
{
    return this.serviceMappings;
};

ServiceMappings.prototype.getXML = function()
{
    var rootNode = cordys.createElementNS(dummyServiceMapping, MAPPING_NAMESPACE_URI, "ServiceMappings");
    var serviceMappingObjs = this.serviceMappings;
    for ( var i = 0; i < serviceMappingObjs.length; i++)
    {
        cordys.appendXMLNode(serviceMappingObjs[i].getXML(), rootNode);
    }
    return rootNode;
};

// search for the particular mapping
ServiceMappings.prototype.containsMapping = function(serviceMapping)
{
    var serviceMappingObjs = this.serviceMappings;
    for ( var i = 0; i < serviceMappingObjs.length; i++)
    {
        if (serviceMappingObjs[i] == serviceMapping)
        {
            return true;
        }
    }
    return false;
};

// search for the service mapping Object for give serviceName and Http Method type
ServiceMappings.prototype.findMapping = function(serviceName, httpType)
{
    var serviceMappingObjs = this.serviceMappings;
    for ( var i = 0; i < serviceMappingObjs.length; i++)
    {
        if (serviceMappingObjs[i].getWSOperation() == serviceName && serviceMappingObjs[i].getHttpMethod() == httpType)
        {
            return serviceMappingObjs[i];
        }
    }
    return null;
};

// returns the service mapping object with given method Name as input
ServiceMappings.prototype.findMappingsWithServiceName = function(serviceName)
{
    var serviceMappingObjs = this.serviceMappings;
    var resultServiceMappingsObj = new ServiceMappings();
    for ( var i = 0; i < serviceMappingObjs.length; i++)
    {
        if (serviceMappingObjs[i].getWSOperation() == serviceName)
        {
            resultServiceMappingsObj.addServiceMapping(serviceMappingObjs[i]);
        }
    }
    return resultServiceMappingsObj;
};

ServiceMappings.parseXML = function(paramXMLNode)
{
    if (!paramXMLNode)
    {
        return null;
    }
    MAPPING_NAMESPACE_URI = paramXMLNode.namespaceURI;
    var serviceMappingsObj = new ServiceMappings();
    var serviceMappingNodes = cordys.selectXMLNodes(paramXMLNode, ".//*[local-name()='ServiceMapping']", "");
    for ( var i = 0; i < serviceMappingNodes.length; i++)
    {
        serviceMappingsObj.addServiceMapping(ServiceMapping.parseXML(serviceMappingNodes[i]));
    }
    return serviceMappingsObj;
};

function ServiceMapping(wsOperation, restURI, httpMethod, resourceName)
{
    this.wsOperation = wsOperation;
    this.restURI = restURI;
    this.httpMethod = httpMethod;
    this.resource = resourceName;
}
ServiceMapping.prototype.setWSOperation = function(wsOperation)
{
    this.wsOperation = wsOperation;
};

ServiceMapping.prototype.getWSOperation = function()
{
    return this.wsOperation;
};

ServiceMapping.prototype.setRESTURI = function(restURI)
{
    this.restURI = restURI;
};

ServiceMapping.prototype.getRESTURI = function()
{
    return this.restURI;
};

ServiceMapping.prototype.setHttpMethod = function(httpMethod)
{
    this.httpMethod = httpMethod;
};

ServiceMapping.prototype.getHttpMethod = function()
{
    return this.httpMethod;
};

ServiceMapping.prototype.setResource = function(resourceName)
{
    this.resource = resourceName;
};

ServiceMapping.prototype.getResource = function()
{
    return this.resource;
};

ServiceMapping.parseXML = function(paramXMLNode)
{
    var serviceMappingNode = paramXMLNode;
    var wsOperation = cordys.getNodeText(serviceMappingNode, ".//*[local-name()='WSOperation']");
    var restURI = cordys.getNodeText(serviceMappingNode, ".//*[local-name()='URIPattern']");
    var httpMethod = cordys.getNodeText(serviceMappingNode, ".//*[local-name()='HttpMethod']");
    var resourceName = cordys.getNodeText(serviceMappingNode, ".//*[local-name()='Resource']");
    return new ServiceMapping(wsOperation, restURI, httpMethod, resourceName);
};

ServiceMapping.prototype.getXML = function()
{
    var ServiceMappingNode = cordys.createElementNS(dummyServiceMapping, MAPPING_NAMESPACE_URI, "ServiceMapping");
    var wsOperationNode = cordys.createElementNS(dummyServiceMapping, MAPPING_NAMESPACE_URI, "WSOperation");
    cordys.setTextContent(wsOperationNode, this.wsOperation);
    var restURINode = cordys.createElementNS(dummyServiceMapping, MAPPING_NAMESPACE_URI, "URIPattern");
    cordys.setTextContent(restURINode, this.restURI);
    var httpMethodNode = cordys.createElementNS(dummyServiceMapping, MAPPING_NAMESPACE_URI, "HttpMethod");
    cordys.setTextContent(httpMethodNode, this.httpMethod);
    var resourceNode = cordys.createElementNS(dummyServiceMapping, MAPPING_NAMESPACE_URI, "Resource");
    cordys.setTextContent(resourceNode, this.resource);
    cordys.appendXMLNode(wsOperationNode, ServiceMappingNode);
    cordys.appendXMLNode(restURINode, ServiceMappingNode);
    cordys.appendXMLNode(httpMethodNode, ServiceMappingNode);
    cordys.appendXMLNode(resourceNode, ServiceMappingNode);
    return ServiceMappingNode;
};
