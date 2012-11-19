var gDocument = null;
var NAMESPACE_URI = "http://schemas.cordys.com/generic/rest/1.0";
var PREFIX = "map:";

function init()
{
}

function RESTMapper(httpGetArg, httpPutArg, httpPostArg, httpDeleteArg, resourceArg, version)
{
    if (httpGetArg)
    {
        this.httpGet = httpGetArg;
    }
    else
    {
        this.httpGet = new HTTPMethod("GET");
    }
    if (httpPutArg)
    {
        this.httpPut = httpPutArg;
    }
    else
    {
        this.httpPut = new HTTPMethod("PUT");
    }
    if (httpPostArg)
    {
        this.httpPost = httpPostArg;
    }
    else
    {
        this.httpPost = new HTTPMethod("POST");
    }
    if (httpDeleteArg)
    {
        this.httpDelete = httpDeleteArg;
    }
    else
    {
        this.httpDelete = new HTTPMethod("DELETE");
    }

    this.resource = resourceArg;
    this.httpMethods = {
        "GET" : this.httpGet,
        "PUT" : this.httpPut,
        "POST" : this.httpPost,
        "DELETE" : this.httpDelete
    };
    this.version = version;
}

RESTMapper.parseXML = function(paramXMLNode)
{
    var prefixpair = new Object();
    prefixpair[PREFIX.substring(0, PREFIX.lastIndexOf(":"))] = NAMESPACE_URI;
    cordys.setXMLNamespaces(paramXMLNode.ownerDocument, prefixpair);
    var restMapperObj;
    var httpGetObjGet = HTTPMethod.parseXML(cordys.selectXMLNode(paramXMLNode, PREFIX + "GET"));
    var httpPutObj = HTTPMethod.parseXML(cordys.selectXMLNode(paramXMLNode, PREFIX + "PUT"));
    var httpPostObj = HTTPMethod.parseXML(cordys.selectXMLNode(paramXMLNode, PREFIX + "POST"));
    var httpdeleteObj = HTTPMethod.parseXML(cordys.selectXMLNode(paramXMLNode, PREFIX + "DELETE"));
    var resourceObj = Resource.parseXML(cordys.selectXMLNode(paramXMLNode, PREFIX + GENERAL_CONSTANTS.RESOURCE_TAG_NAME));
    var version = paramXMLNode.getAttribute("version");
    restMapperObj = new RESTMapper(httpGetObjGet, httpPutObj, httpPostObj, httpdeleteObj, resourceObj, version);
    return restMapperObj;
};

RESTMapper.prototype.getXML = function()
{
    var restMapperNode = cordys.createElementNS(gDocument, NAMESPACE_URI, GENERAL_CONSTANTS.RESTMAPPING_TAG_NAME);
    cordys.appendXMLNode(this.resource.getXML(), restMapperNode);
    var httpMethods = this.httpMethods;
    for ( var i in httpMethods)
    {
        if (httpMethods[i])
        {
            cordys.appendXMLNode(httpMethods[i].getXML(), restMapperNode);
        }
    }
    cordys.setXMLAttribute(restMapperNode, NAMESPACE_URI, "version", this.version);
    return restMapperNode;
};

RESTMapper.prototype.getHttpTypeOfWSOperation = function(wsOperation) // only one WSOperation will be aviailble for the object wsOperation
{
    var httpMethods = this.httpMethods;
    for ( var httpType in httpMethods)
    {
        if (httpMethods[httpType] && httpMethods[httpType].containsOperation(wsOperation))
        {
            return httpType;
        }
    }
    return null;
};

RESTMapper.prototype.getAvailHttpTypeOfWSOperation = function(methodName, namespace) // Note: many WSOperations will be available for the same namespace and methodname
{
    var httpMethods = this.httpMethods;
    for ( var httpType in httpMethods)
    {
        if (httpMethods[httpType] && httpMethods[httpType].getWSOperation(methodName, namespace))
        {
            return httpType;
        }
    }
    return null;
};

RESTMapper.prototype.getWSOperation = function(methodName, namespace, httpType)
{
    var httpMethods = this.httpMethods;
    return httpMethods[httpType].getWSOperation(methodName, namespace);
};

RESTMapper.prototype.getWSOperationsByUriPatternAndHttpType = function(uriPatternArg, httpType)
{
    var resultArr = new Array();
    var wsOperations = this.httpMethods[httpType].getAllWSOperations();
    for ( var key in wsOperations)
    {
        if (wsOperations[key].getURIPattern() == uriPatternArg)
        {
            resultArr.push(wsOperations[key]);
        }
    }
    return resultArr;
};

RESTMapper.prototype.addWSOperation = function(wsOperation, httyType)
{
    this.httpMethods[httyType].addWSOperation(wsOperation);
};

RESTMapper.prototype.getResourceNSPrefixPairs = function() // returns prefixes and namespaces as "key => value" pairs.
{
    if (this.resource.nsPrefixList)
    {
        return this.resource.nsPrefixList.getNSPrefixPairs();
    }
    else
    {
        return new Array();
    }
};

RESTMapper.prototype.setResourceNSPrefixList = function(nsPrefixListArg)
{
    this.resource.nsPrefixList = nsPrefixListArg;
};

function Resource(resourceNameArg, nsPrefixListObj)
{
    this.resourceName = resourceNameArg;
    this.nsPrefixList = nsPrefixListObj;
}

Resource.parseXML = function(paramXMLNode)
{
    var resourceName = cordys.getNodeText(paramXMLNode, PREFIX + GENERAL_CONSTANTS.RESOURCE_NAME_TAG_NAME);
    var nsPrefixesNode = cordys.selectXMLNode(paramXMLNode, PREFIX + "NamespacePrefixes");
    var nsPrefixListObj = NamespacePrefixList.parseXML(nsPrefixesNode);
    return new Resource(resourceName, nsPrefixListObj);
};

Resource.prototype.getXML = function()
{
    var resourceNode = cordys.createElementNS(gDocument, NAMESPACE_URI, GENERAL_CONSTANTS.RESOURCE_TAG_NAME);
    var resourceNameNode = cordys.createElementNS(gDocument, NAMESPACE_URI, GENERAL_CONSTANTS.RESOURCE_NAME_TAG_NAME);
    cordys.setTextContent(resourceNameNode, this.resourceName);
    cordys.appendXMLNode(resourceNameNode, resourceNode);
    cordys.appendXMLNode(this.nsPrefixList.getXML(), resourceNode);
    return resourceNode;
};

function HTTPMethod(type)
{
    this.operations = new Object();
    this.type = type;
}

HTTPMethod.prototype.addWSOperation = function(wsOperation)
{
    var key = wsOperation.getNamespace() + "/" + wsOperation.getName();
    if (this.operations[key])
    {
        throw new Error("WSOperation exists already !");
    }
    this.operations[key] = wsOperation;
};

HTTPMethod.prototype.removeWSOperation = function(wsOperation)
{
    var key = wsOperation.getNamespace() + "/" + wsOperation.getName();
    if (!this.operations[key])
    {
        throw new Error("WSOperation not found !");
    }
    delete this.operations[key];
};

HTTPMethod.prototype.getWSOperation = function(methodName, namespace)
{
    var key = namespace + "/" + methodName;
    if (this.operations[key])
    {
        return this.operations[key];
    }
    else
    {
        return null;
    }
};

HTTPMethod.prototype.getAllWSOperations = function()
{
    return this.operations;
};

HTTPMethod.parseXML = function(paramXMLNode)
{
    if (!paramXMLNode)
    {
        return null;
    }
    var methodType = paramXMLNode.nodeName;
    var httpMethodObj = new HTTPMethod(methodType);
    var wsOperationNodes = cordys.selectXMLNodes(paramXMLNode, PREFIX + GENERAL_CONSTANTS.WSOPERATION_TAG_NAME);
    for ( var i = 0; i < wsOperationNodes.length; i++)
    {
        httpMethodObj.addWSOperation(WSOperation.parseXML(wsOperationNodes[i]));
    }
    return httpMethodObj;
};

HTTPMethod.prototype.getXML = function()
{
    var rootNode = cordys.createElementNS(gDocument, NAMESPACE_URI, this.type);
    var wsOperationObjs = this.operations;
    for ( var i in wsOperationObjs)
        cordys.appendXMLNode(wsOperationObjs[i].getXML(), rootNode);
    return rootNode;
};

HTTPMethod.prototype.containsOperation = function(wsOperation)
{
    var wsOperationObjs = this.operations;
    for ( var i in wsOperationObjs)
        if (wsOperationObjs[i] == wsOperation)
        {
            return true;
        }
    return false;
};

function WSOperation(nameArg, namespaceArg, uriPatternArg, inputArg, outputArg)
{
    this.name = nameArg;
    this.namespace = namespaceArg;
    this.uriPattern = uriPatternArg;
    this.input = inputArg;
    this.output = outputArg;
    this.httpBodyInput = null;
    this.soapRequest = null;
    this.recentlyCreated = false; // this is used only for UI Tooling to identify whether the WSOperation is already present in the
    // mapping xml or created just now
}

WSOperation.prototype.setInput = function(inputObj)
{
    this.input = inputObj;
};

WSOperation.prototype.getInput = function()
{
    return this.input;
};

WSOperation.prototype.setOutput = function(outputObj)
{
    this.output = outputObj;
};

WSOperation.prototype.getOutput = function()
{
    return this.output;
};

WSOperation.prototype.getName = function()
{
    return this.name;
};

WSOperation.prototype.getNamespace = function()
{
    return this.namespace;
};

WSOperation.prototype.getURIPattern = function()
{
    return this.uriPattern;
};

WSOperation.prototype.getHttpBodyInput = function()
{
    return this.httpBodyInput;
};

WSOperation.prototype.setHttpBodyInput = function(httpBodyInputArg)
{
    this.httpBodyInput = httpBodyInputArg;
};

WSOperation.prototype.setURIPattern = function(uriPatternArg)
{
    this.uriPattern = uriPatternArg;
};

WSOperation.prototype.setSoapRequest = function(soapReqNodeArg)
{
    if (soapReqNodeArg.nodeType == 9)
    {
        soapReqNodeArg = soapReqNodeArg.documentElement;
    }
    this.soapRequest = soapReqNodeArg;
};

WSOperation.prototype.getSoapRequest = function()
{
    return this.soapRequest;
};

WSOperation.prototype.setRecentlyCreated = function(recentlyCreatedFlag)
{
    this.recentlyCreated = recentlyCreatedFlag;
};

WSOperation.prototype.istRecentlyCreated = function()
{
    return this.recentlyCreated;
};

WSOperation.prototype.setInput = function(inputObj)
{
    this.input = inputObj;
};

WSOperation.parseXML = function(paramXMLNode)
{
    var operationName = cordys.getNodeText(paramXMLNode, PREFIX + GENERAL_CONSTANTS.WSOPERATION_NAME_TAG_NAME);
    var namespace = cordys.getNodeText(paramXMLNode, PREFIX + GENERAL_CONSTANTS.WSOPERATION_NAMESPACE_TAG_NAME);
    var uriPattern = paramXMLNode.getAttribute(GENERAL_CONSTANTS.WSOPERATION_URI_PATTERN_NAME);
    var inputNode = cordys.selectXMLNode(paramXMLNode, PREFIX + GENERAL_CONSTANTS.WSOPERATION_INPUT_TAG_NAME);
    var outputNode = cordys.selectXMLNode(paramXMLNode, PREFIX + GENERAL_CONSTANTS.WSOPERATION_OUTPUT_TAG_NAME);
    var httpBodyInputNode = cordys.selectXMLNode(paramXMLNode, PREFIX + GENERAL_CONSTANTS.HTTP_BODY_INPUT_TAG_NAME);
    var soapReqNode = cordys.selectXMLNode(paramXMLNode, PREFIX + GENERAL_CONSTANTS.SOAP_REQUEST_TAG_NAME);
    var wsOperation = new WSOperation(operationName, namespace, uriPattern);
    var x = Input.parseXML(inputNode);
    wsOperation.setInput(x);
    wsOperation.setOutput(Output.parseXML(outputNode));
    wsOperation.setRecentlyCreated(false);
    if (httpBodyInputNode)
    {
        wsOperation.setHttpBodyInput(httpBodyInputNode.firstChild);
    }
    if (soapReqNode)
    {
        wsOperation.setSoapRequest(soapReqNode.firstChild);
    }
    return wsOperation;
};

WSOperation.prototype.getXML = function()
{
    if (!this.input || !this.output)
    {
        throw new Error("Invalid Input/Output/HTTP Type for WS Operation.");
    }
    var wsNode = cordys.createElementNS(gDocument, NAMESPACE_URI, GENERAL_CONSTANTS.WSOPERATION_TAG_NAME);
    var operationNameNode = cordys.createElementNS(gDocument, NAMESPACE_URI, GENERAL_CONSTANTS.WSOPERATION_NAME_TAG_NAME);
    cordys.setTextContent(operationNameNode, this.name);
    cordys.appendXMLNode(operationNameNode, wsNode);
    var namespaceNode = cordys.createElementNS(gDocument, NAMESPACE_URI, GENERAL_CONSTANTS.WSOPERATION_NAMESPACE_TAG_NAME);
    cordys.setTextContent(namespaceNode, this.namespace);
    cordys.appendXMLNode(namespaceNode, wsNode);
    cordys.setXMLAttribute(wsNode, NAMESPACE_URI, GENERAL_CONSTANTS.WSOPERATION_URI_PATTERN_NAME, this.uriPattern);
    if (this.httpBodyInput)
    {
        var httpBodyInputNode = cordys.createElementNS(gDocument, NAMESPACE_URI, GENERAL_CONSTANTS.HTTP_BODY_INPUT_TAG_NAME);
        var tempNode = this.httpBodyInput;
        if (tempNode.nodeType == 9)
        {
            tempNode = tempNode.documentElement;
        }
        cordys.appendXMLNode(tempNode.cloneNode(true), httpBodyInputNode);
        cordys.appendXMLNode(httpBodyInputNode, wsNode);
    }
    if (this.soapRequest)
    {
        var soapRequestNode = cordys.createElementNS(gDocument, NAMESPACE_URI, GENERAL_CONSTANTS.SOAP_REQUEST_TAG_NAME);
        var tempNode = this.soapRequest;
        if (tempNode.nodeType == 9)
        {
            tempNode = tempNode.documentElement;
        }
        cordys.appendXMLNode(tempNode.cloneNode(true), soapRequestNode);
        cordys.appendXMLNode(soapRequestNode, wsNode);
    }
    cordys.appendXMLNode(this.input.getXML(), wsNode);
    cordys.appendXMLNode(this.output.getXML(), wsNode);
    return wsNode;
};

function Entry(rootName, keyElementName, valueElementName, key, value)
{
    this.rootName = rootName;
    this.keyElementName = keyElementName;
    this.valueElementName = valueElementName;
    this.key = key;
    this.value = value;
    if (typeof (value) == "object" && value.nodeType)
    {
        this.valueType = GENERAL_CONSTANTS.XML;
    }
    else
    {
        this.valueType = GENERAL_CONSTANTS.TEXT;
    }
}

Entry.prototype.getKeyElementName = function()
{
    return keyElementName;
};

Entry.prototype.getValueElementName = function()
{
    return valueElementName;
};

Entry.prototype.getKey = function()
{
    return this.key;
};

Entry.prototype.getValue = function()
{
    return this.value;
};

Entry.prototype.setValue = function(newVal)
{
    this.value = newVal;
};

Entry.prototype.getXML = function()
{

    var rootnode = cordys.createElementNS(gDocument, NAMESPACE_URI, this.rootName);
    var keyNode = cordys.createElementNS(gDocument, NAMESPACE_URI, this.keyElementName);
    cordys.setTextContent(keyNode, this.key);
    var valueNode = cordys.createElementNS(gDocument, NAMESPACE_URI, this.valueElementName);
    if (this.valueType == GENERAL_CONSTANTS.TEXT)
    {
        cordys.setTextContent(valueNode, this.value);
    }
    else
    {
        var xmlnode = null;
        if (this.value.nodeType == 9)
        {
            xmlnode = this.value.documentElement;
        }
        else
        {
            xmlnode = this.value;
        }
        cordys.appendXMLNode(xmlnode.cloneNode(true), valueNode);
    }
    cordys.appendXMLNode(keyNode, rootnode);
    cordys.appendXMLNode(valueNode, rootnode);
    return rootnode;
};

Entry.parseXML = function(paramXMLNode)
{
    var xmlNode = paramXMLNode;
    var rootNodeName = xmlNode.nodeName;
    if (xmlNode.childNodes.length != 2)
    {
        throw new Error("Invalid Entry Node");
    }
    var keyElementNode = xmlNode.firstChild;
    var keyElementName = keyElementNode.nodeName;
    var keyContent = cordys.getTextContent(keyElementNode.firstChild);
    var valueElementNode = xmlNode.lastChild;
    var valueElementName = valueElementNode.nodeName;
    var valueType = MappingUtil.getType(valueElementNode);
    var valueContent;
    if (valueType == GENERAL_CONSTANTS.TEXT)
    {
        valueContent = cordys.getTextContent(valueElementNode.firstChild);
    }
    else
    {
        valueContent = valueElementNode.firstChild;
    }
    var entryObject = new Entry(rootNodeName, keyElementName, valueElementName, keyContent, valueContent);
    return entryObject;
};

function Filter(type, value)
{
    if (type && value)
    {
        this.entry = new Entry("Filter", "Type", "Value", type, value);
    }
}

Filter.parseXML = function(paramXMLNode)
{
    if (paramXMLNode)
    {
        var filterObj = new Filter();
        filterObj.entry = Entry.parseXML(paramXMLNode);
        return filterObj;
    }
    else
    {
        return null;
    }
};

Filter.prototype.getXML = function()
{
    return this.entry.getXML();
};
// ---------- Start URL Element and value //XPath => /RESTMapping/PUT/WSOperation/(Input or Output )/Elements

function Element(name, value)
{
    if (name && value)
    {
        this.entry = new Entry("Element", "Name", "Value", name, value);
    }
}

/*
 * returns the array of Element Objects present in the xml node
 */
Element.parseXML = function(paramXMLNode)
{
    var entryObj = Entry.parseXML(paramXMLNode);
    var elementObj = new Element();
    elementObj.entry = entryObj;
    return elementObj;
};

Element.prototype.getXML = function()
{
    return this.entry.getXML();
};

// ---------- Start Namespace - Prefix //XPath => /RESTMapping/Resource/NamespacePrefixes/NamespacePrefix
function NamespacePrefix(prefix, namespace)
{
    if (prefix && namespace)
    {
        this.entry = new Entry("NamespacePrefix", "Prefix", "Namespace", prefix, namespace);
    }
}

/*
 * returns the array of Namespace-Prefix Objects present in the xml node
 */
NamespacePrefix.parseXML = function(paramXMLNode)
{
    var namespacePrefixObj = new NamespacePrefix();
    var entryObj = Entry.parseXML(paramXMLNode);
    namespacePrefixObj.entry = entryObj;
    return namespacePrefixObj;
};

NamespacePrefix.prototype.getXML = function()
{
    return this.entry.getXML();
};

function NamespacePrefixList(nsPrefixesArray)
{
    if (!nsPrefixesArray)
    {
        nsPrefixesArray = new Array();
    }
    this.NSPrefixes = nsPrefixesArray;
    this.entries = nsPrefixesArray;
}

NamespacePrefixList.prototype.addNamespacePrefix = function(prefix, namespace)
{
    var nsAndPrefixObj = new NamespacePrefix(prefix, namespace);
    this.entries.push(nsAndPrefixObj);
};

NamespacePrefixList.prototype.getNSPrefixPairs = function()
{
    var prefixes = this.NSPrefixes;
    var pairs = new Object();
    for ( var i = 0; i < prefixes.length; i++)
    {
        pairs[prefixes[i].entry.key] = prefixes[i].entry.value;
    }
    return pairs;
};

NamespacePrefixList.parseXML = function(paramXMLNode)
{
    return MappingUtil.parseEntryListfromXML(paramXMLNode, NamespacePrefixList, NamespacePrefix, "NamespacePrefix");
};

NamespacePrefixList.prototype.getXML = function()
{
    return MappingUtil.getEntryListXML(this, "NamespacePrefixes");
};
// ---------- End Namespace - Prefix

/*
 * filter - filter object elements = elements array
 */
function Input(filter, elementsArray)
{
    this.filter = filter;
    this.elementsArr = elementsArray;
}

Input.parseXML = function(paramXMLNode)
{
    var filterNode = cordys.selectXMLNode(paramXMLNode, PREFIX + GENERAL_CONSTANTS.FILTER_TAG_NAME);
    var filterObj = Filter.parseXML(filterNode);
    var elementNodes = cordys.selectXMLNodes(paramXMLNode, PREFIX + GENERAL_CONSTANTS.ELEMENT_TAG_NAME);
    var elementObjsArray = [];
    for ( var i = 0; i < elementNodes.length; i++)
    {
        elementObjsArray.push(Element.parseXML(elementNodes[i]));
    }
    if (filterObj || elementObjsArray.length > 0)
    {
        return new Input(filterObj, elementObjsArray);
    }
    else
    {
        return new Input(null, null);
    }
};

Input.prototype.getXML = function()
{
    var inputNode = cordys.createElementNS(gDocument, NAMESPACE_URI, GENERAL_CONSTANTS.WSOPERATION_INPUT_TAG_NAME);
    var elementObjsArray = this.elementsArr;
    for ( var i = 0; elementObjsArray && i < elementObjsArray.length; i++)
    {
        cordys.appendXMLNode(elementObjsArray[i].getXML(), inputNode);
    }
    if (this.filter)
    {
        cordys.appendXMLNode(this.filter.getXML(), inputNode);
    }
    return inputNode;
};

/*
 * filter - filter object
 */
function Output(filter)
{
    this.filter = filter;
}

Output.parseXML = function(paramXMLNode)
{
    var filterNode = cordys.selectXMLNode(paramXMLNode, PREFIX + GENERAL_CONSTANTS.FILTER_TAG_NAME);
    var filterObj = Filter.parseXML(filterNode);
    if (filterObj)
    {
        return new Output(filterObj);
    }
    return null;
};

Output.prototype.getXML = function()
{
    var outputNode = cordys.createElementNS(gDocument, NAMESPACE_URI, GENERAL_CONSTANTS.WSOPERATION_OUTPUT_TAG_NAME);
    if (this.filter)
    {
        cordys.appendXMLNode(this.filter.getXML(), outputNode);
    }
    return outputNode;
};

GENERAL_CONSTANTS = {
    TEXT : "TEXT",
    XML : "XML",
    FILTER_TAG_NAME : "Filter",
    ELEMENT_TAG_NAME : "Element",
    WSOPERATION_NAME_TAG_NAME : "Name",
    WSOPERATION_NAMESPACE_TAG_NAME : "Namespace",
    WSOPERATION_URI_PATTERN_NAME : "URIPattern",
    WSOPERATION_INPUT_TAG_NAME : "Input",
    WSOPERATION_OUTPUT_TAG_NAME : "Output",
    RESOURCE_TAG_NAME : "Resource",
    RESOURCE_NAME_TAG_NAME : "Name",
    WSOPERATION_TAG_NAME : "WSOperation",
    RESTMAPPING_TAG_NAME : "RESTMapping",
    HTTP_BODY_INPUT_TAG_NAME : "HTTPBody",
    SOAP_REQUEST_TAG_NAME : "SOAPRequest",
    CUR_VERSION : "1.0"
};

function MappingUtil()
{
}

MappingUtil.getType = function(valueXMLNode)
{
    if (valueXMLNode.firstChild.nodeType == 3)
    {
        return GENERAL_CONSTANTS.TEXT;
    }
    if (valueXMLNode.firstChild.nodeType == 1)
    {
        return GENERAL_CONSTANTS.XML;
    }
};

MappingUtil.parseEntryListfromXML = function(paramXMLNode, parentClassName, childClassName, childNodeName)
{
    var nodes = cordys.selectXMLNodes(paramXMLNode, PREFIX + childNodeName);
    if (nodes.length == 0)
    {
        new parentClassName(null);
    }
    var childrenObjsArray = [];
    for ( var i = 0; i < nodes.length; i++)
    {
        var childClassObj = childClassName.parseXML(nodes[i]);
        if (childClassObj)
        {
            childrenObjsArray.push(childClassObj);
        }
    }
    return new parentClassName(childrenObjsArray);
};

MappingUtil.getEntryListXML = function(entryListObj, parentClassName)
{
    var entriesArray = entryListObj.entries;
    var entriesNode = cordys.createElementNS(gDocument, NAMESPACE_URI, parentClassName);
    for ( var i = 0; i < entriesArray.length; i++)
    {
        var entryNode = entriesArray[i].getXML();
        if (entryNode)
        {
            cordys.appendXMLNode(entryNode, entriesNode);
        }
    }
    return entriesNode;
};

function MappingManager()
{
    this.restMappers = new Object();
}

MappingManager.prototype.parseConfiguration = function(confNode)
{
    var restMapperObj = RESTMapper.parseXML(confNode);
    if (!this.restMappers[restMapperObj.resource.resourceName])
    {
        this.restMappers[restMapperObj.resource.resourceName] = restMapperObj;
    }
    return restMapperObj;
};

MappingManager.prototype.createRESTMapper = function(resourceObjArg)
{
    var restMapperObj = new RESTMapper(null, null, null, null, resourceObjArg, GENERAL_CONSTANTS.CUR_VERSION);
    if (!this.restMappers[restMapperObj.resource.resourceName])
    {
        this.restMappers[restMapperObj.resource.resourceName] = restMapperObj;
    }
    return restMapperObj;
};

MappingManager.prototype.getRESTMapper = function(resourceName)
{
    return this.restMappers[resourceName];
    ;
};

MappingManager.prototype.createURLParams = function(xpathParamArray, urlLocArr)
{
    if (xpathParamArray.length != urlLocArr.length)
    {
        throw new Error("Mismatch between XPath Params and URL Locations while creating URL Params.");
    }
    var elementObjects = new Array();
    for ( var i = 0; i < xpathParamArray.length; i++)
    {
        elementObjects.push(new Element(xpathParamArray[i], urlLocArr[i]));
    }
    return elementObjects;
};

MappingManager.prototype.createInput = function(xsltDocument, urlParams /* Elements[] */)
{
    var filterObj = null;
    if (xsltDocument)
    {
        filterObj = new Filter("xslt", xsltDocument);
    }
    return new Input(filterObj, urlParams);
};

MappingManager.prototype.createOutput = function(xsltDocument)
{
    var xsltNode = xsltDocument.documentElement;
    var filterObj = new Filter("xslt", xsltNode);
    return new Output(filterObj);
};

MappingManager.prototype.createWSOperation = function(operationName, namespace, uriPattern)
{
    var wsOperationObj = new WSOperation(operationName, namespace, uriPattern, null, null);
    wsOperationObj.setRecentlyCreated(true);
    return wsOperationObj;
};

MappingManager.prototype.createResource = function(resourceName, prefixAndNSObjs)
{
    return new Resource(resourceName, this.createNamespacePrefixList(prefixAndNSObjs));
};

MappingManager.prototype.createNamespacePrefixList = function(prefixAndNSObjs)
{

    var NamespacePrefixesArray = new Array();
    for ( var prefix in prefixAndNSObjs)
    {
        NamespacePrefixesArray.push(new NamespacePrefix(prefix, prefixAndNSObjs[prefix]));
    }
    return new NamespacePrefixList(NamespacePrefixesArray);
};
