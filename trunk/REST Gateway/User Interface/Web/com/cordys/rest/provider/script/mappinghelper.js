var gMappingManager = null;
var gResources = new Object();

function initMappingManager()
{
    gMappingManager = new MappingManager();
    gDocument = cordys.getXMLDocument();
}

function clearMappingData()
{
    gMappingManager = null;
    gDocument = null;
    gResources = null;
}

function buildMappingConfigAndShow(methodName, namespace)
{
    initMappingManager();
    var resourceName = getResource(methodName, namespace);
    var httpType;
    var confNode = getMappingTemplatefromXmlStore(resourceName);
    if (confNode)
    {
        try
        {
            gMappingManager.parseConfiguration(confNode);
        }
        catch (e)
        {
            application.showError("Error in parsing the Resource \"" + resourceName + "\"");
            return;
        }
    }
    else
    {
        var resouceObj = gMappingManager.createResource(resourceName, null);
        gMappingManager.createRESTMapper(resouceObj);
    }
    var restMapper = getRESTMapperByMethodInfo(methodName, namespace);
    var availableHttpType = restMapper.getAvailHttpTypeOfWSOperation(methodName, namespace);
    if (availableHttpType)
    {
        inResource.disable();
        clearRestMethodDetails();
    }
    else
    {
        inResource.enable();
        httpType = getSuggestedHttpType(methodName);
        try
        {
            createMappingConf(methodName, namespace, httpType);
        }
        catch (e)
        {
            applicatmaion.showError("Error in generating the REST URI for the Operation " + methodName);
            return;
        }
        createMappingConf(methodName, namespace, httpType);
        showRestMethodDetails(methodName, namespace, httpType);
        showSoapMethodDetails(methodName, namespace, httpType);
    }

}

function createMappingConf(methodName, namespace, httpType)
{
    var restMapper = getRESTMapperByMethodInfo(methodName, namespace);
    var wsOperation = null;
    wsOperation = restMapper.getWSOperation(methodName, namespace, httpType);
    if (wsOperation)
    {
        return null; // already WS Operation exists.
    }
    var existingNSPairs = restMapper.getResourceNSPrefixPairs();
    var soapReq = extractSOAPfromWSDL("request");
    XPathUtil.setNSPrefixes(existingNSPairs);
    var result = XPathUtil.extractXPathsfromXML(soapReq, "", true);
    var urlLocs = new Array();
    var xpathList = result.xpathList;
    for ( var i = 0; i < xpathList.length; i++)
    {
        urlLocs[i] = "$URL$:{" + i + "}";
    }
    var urlElements = gMappingManager.createURLParams(xpathList, urlLocs);
    var prefixList = result.nsPrefixList;
    restMapper.setResourceNSPrefixList(gMappingManager.createNamespacePrefixList(prefixList));
    if (result.aborted)
    {
        btnShowAllParams.enable();
    }
    else
    {
        btnShowAllParams.disable();
    }
    var requestXsltDoc;
    var httpBodyInputObj;
    if (httpType == "PUT" || httpType == "POST") // Http Request Body and Xslt are needed only for PUT and POST.
    {
        requestXsltDoc = cordys.cloneXMLDocument(ID_defaultXSLT.XMLDocument);
        httpBodyInputObj = soapReq;
    }
    else
    {
        requestXsltDoc = null;
        httpBodyInputObj = null;
    }
    var inputObject = gMappingManager.createInput(requestXsltDoc, urlElements);
    var outputObject = gMappingManager.createOutput(cordys.cloneXMLDocument(ID_defaultXSLT.XMLDocument));
    wsOperation = gMappingManager.createWSOperation(methodName, namespace, buildRestUriPattern(methodName, namespace, urlLocs));
    wsOperation.setInput(inputObject);
    wsOperation.setHttpBodyInput(httpBodyInputObj);
    wsOperation.setSoapRequest(soapReq);
    wsOperation.setOutput(outputObject);
    restMapper.addWSOperation(wsOperation, httpType);
}

function generateUrlParams(methodName, namespace, httpType, bshowAll)
{
    var restMapper = getRESTMapperByMethodInfo(methodName, namespace);
    var wsOperation = restMapper.getWSOperation(methodName, namespace, httpType);
    if (!wsOperation)
    {
        throw new Error("WSOperation for " + methodName + " not found"); // already WS Operation exists.
    }
    var existingNSPairs = restMapper.getResourceNSPrefixPairs();
    var soapReq = getSoapRequestXML();
    if (!isValidXML(soapReq))
    {
        application.showError("Web Service Operation SOAP Request is not a valid XML. Please verify.");
        return;
    }
    soapReq = soapReq.documentElement;
    XPathUtil.setNSPrefixes(existingNSPairs);
    var result = XPathUtil.extractXPathsfromXML(soapReq, "", !bshowAll);
    var urlLocs = new Array();
    var xpathList = result.xpathList;
    for ( var i = 0; i < xpathList.length; i++)
    {
        urlLocs[i] = "$URL$:{" + i + "}";
    }
    btnShowAllParams.disable();
    var urlElements = gMappingManager.createURLParams(xpathList, urlLocs);
    var prefixList = result.nsPrefixList;
    restMapper.setResourceNSPrefixList(gMappingManager.createNamespacePrefixList(prefixList));
    wsOperation.getInput().elementsArr = urlElements;
    fillRestParamsTable(wsOperation);
    fillNamespaceTable(restMapper.getResourceNSPrefixPairs());
    var result = getCurUrlParamsAndLoc();
    var uriPattern = buildRestUriPattern(globalObj.curMethodName, globalObj.curLabeledUri, result.urlLocs);
    outRestUrl.setValue(convertUriPattern2StdRestUrl(uriPattern));
    if (bshowAll)
    {
        application.notify("Url parameters have been generated successfully.");
    }
}

function showSoapMethodDetails(methodName, namespace, httpType)
{
    var wsOperation = getWSOperationByMethodInfo(methodName, namespace, httpType);
    var soapReq = wsOperation.getSoapRequest();
    textMethodRequest.setValue(xmlUtil.xml2nicestring(soapReq, 0));
    outMethodName.setValue(methodName);
    outNamespace.setValue(namespace);
}

function showRestMethodDetails(methodName, namespace, httpType)
{
    RESTParamsModel.clear();
    RESTParamsModel.refreshAllViews();
    var resourceName = getResource(methodName, namespace);
    var restMapper = getRESTMapperByMethodInfo(methodName, namespace);
    var wsOperation = restMapper.getWSOperation(methodName, namespace, httpType);
    var uriPattern = wsOperation.getURIPattern();
    fillRestParamsTable(wsOperation);
    fillNamespaceTable(restMapper.getResourceNSPrefixPairs());
    var customizesdSoapReqNode = wsOperation.getHttpBodyInput();
    if (customizesdSoapReqNode)
    {
        checkReqbody.setValue(1);
        fillMethodReqBodyDetails(customizesdSoapReqNode, wsOperation.getInput().filter.entry.getValue());
    }
    else
    {
        checkReqbody.setValue(0);
        textRestReqBodyXml.setValue("");
        textReqXslt.setValue("");
        textTranslatedReq.setValue("");
    }

    var originalSoapRes = extractSOAPfromWSDL("response");
    fillMethodResBodyDetails(originalSoapRes, wsOperation.getOutput().filter.entry.getValue());
    checkRespBody.setValue(1);
    selHttpType.setValue(httpType);
    selHttpType_Change();
    outRestUrl.setValue(convertUriPattern2StdRestUrl(uriPattern));
    inResource.systemMode = true;
    inResource.setValue(resourceName);
    inResource.systemMode = false;
}

function clearRestMethodDetails()
{
    inResource.systemMode = true;
    inResource.setValue("");
    inResource.systemMode = false;
    selHttpType.systemClearMode = true;
    selHttpType.setValue("none");
    selHttpType.systemClearMode = false;
    outRestUrl.setValue("");
    NSInfoModel.clear();
    NSInfoModel.refreshAllViews();
    RESTParamsModel.clear();
    RESTParamsModel.refreshAllViews();
    textRestReqBodyXml.setValue("");
    textRestReqBodyXml.setLabel(textRestReqBodyXml.defaultLabel);
    textReqXslt.setValue("");
    textReqXslt.setLabel(textReqXslt.defaultLabel);
    textTranslatedReq.setValue("");
    textTranslatedReq.setLabel(textTranslatedReq.defaultLabel);
    textSoapResBodyXml.setValue("");
    textSoapResBodyXml.setLabel(textSoapResBodyXml.defaultLabel);
    textResXslt.setValue("");
    textResXslt.setLabel(textResXslt.defaultLabel);
    textTranslatedRes.setValue("");
    textTranslatedRes.setLabel(textTranslatedRes.defaultLabel);
}

function fillRestParamsTable(wsOperation)
{
    RESTParamsModel.clear();
    RESTParamsModel.refreshAllViews();
    var elements = wsOperation.getInput().elementsArr;
    for ( var i = 0; elements && i < elements.length; i++)
    {
        tableRESTParams.create();
        var xpath = elements[i].entry.getKey();
        var urlLoc = elements[i].entry.getValue();
        inpXpath[i + 1].setValue(xpath);
        inpSoapParam[i + 1].setValue(XPathUtil.getParamNameFromXPath(xpath));
        inpRESTParam[i + 1].setValue(urlLoc);
    }
    tableRESTParams.parentNode.scrollTop = "0px";
}

function fillNamespaceTable(prefixArg)
{
    NSInfoModel.clear();
    NSInfoModel.refreshAllViews();
    var prefixes = prefixArg;
    var i = 0;
    for ( var key in prefixes)
    {
        tableNSInfo.create();
        outColPrefix[i + 1].setValue(key);
        outColNamespace[i + 1].setValue(prefixes[key]);
        i++;
    }
}

function buildRestUriPattern(methodName, namespace, urlLocs)
{
    var resourceName = getResource(methodName, namespace);
    var url = "/" + resourceName;
    for ( var i = 0; urlLocs && i < urlLocs.length; i++)
    {
        url += "/" + urlLocs[i].substring("$URL$:".length);
    }
    return url;
}

// -- start-- methods to change the WSOperation internals
function changeXsltOfMethod(methodName, namespace, httpType, newXsltDoc, inputOutputType)
{
    var wsOperation = getWSOperationByMethodInfo(methodName, namespace, httpType);
    var inOutObj;
    if (inputOutputType == "input")
    {
        inOutObj = wsOperation.getInput();
    }
    else
    {
        inOutObj = wsOperation.getOutput();
    }
    if (newXsltDoc)
    {
        if (!inOutObj.filter)
        {
            inOutObj.filter = new Filter("xslt", newXsltDoc);
        }
        else
        {
            inOutObj.filter.entry.setValue(newXsltDoc);
        }
    }
    else
    {
        inOutObj.filter = null;
    }
}

function changeHttpBodyInputOfMethod(methodName, namespace, httpType, newXmlDoc)
{
    var wsOperation = getWSOperationByMethodInfo(methodName, namespace, httpType);
    wsOperation.setHttpBodyInput(newXmlDoc);
}

function changeUrlParamsOfMethod(methodName, namespace, httpType, newUrlParams, newUrlLocs)
{
    var wsOperation = getWSOperationByMethodInfo(methodName, namespace, httpType);
    var elementObjsArr = gMappingManager.createURLParams(newUrlParams, newUrlLocs);
    wsOperation.getInput().elementsArr = elementObjsArr;
}

function changeRestUriPatternOfMethod(methodName, namespace, httpType, uriPattern)
{
    var wsOperation = getWSOperationByMethodInfo(methodName, namespace, httpType);
    wsOperation.setURIPattern(uriPattern);
}

function changeSoapRequestOfMethod(methodName, namespace, httpType, soapRequest)
{
    var wsOperation = getWSOperationByMethodInfo(methodName, namespace, httpType);
    wsOperation.setSoapRequest(soapRequest);
}
// -- end -- methods to change the WSOperation internals

function getSoapRequest(methodName, namespace, httpType)
{
    var wsOperation = getWSOperationByMethodInfo(methodName, namespace, httpType);
    return wsOperation.getSoapRequest();
}

function checkAndCreateWsOperationForHttpType(methodName, namespace, oldHttpType, newHttpType)
{
    var restMapper = getRESTMapperByMethodInfo(methodName, namespace);
    if (oldHttpType != "none")
    {
        var oldWsOperation = restMapper.getWSOperation(methodName, namespace, oldHttpType);
        // if(!oldWsOperation)
        // throw new Error("Mapping for the method "+ methodName +" not found.");
        if (oldWsOperation && oldWsOperation.istRecentlyCreated())
        {
            restMapper.httpMethods[oldHttpType].removeWSOperation(oldWsOperation);
        }
    }
    if (newHttpType == "none")
    {
        clearRestMethodDetails();
    }
    else
    {
        var newWsOperation = restMapper.getWSOperation(methodName, namespace, newHttpType); // checks whether WSOperation for the new http exists alreay.
        if (!newWsOperation)
        {
            createMappingConf(methodName, namespace, newHttpType); // if not present, create WSOperation
        }
        showRestMethodDetails(methodName, namespace, newHttpType);
        showSoapMethodDetails(methodName, namespace, newHttpType);
    }
}

function getSuggestedHttpType(methodName)
{
    if (methodName.search(/Get/i) == 0 || methodName.search(/Read/i) == 0)
    {
        return "GET";
    }
    else
        if (methodName.indexOf("Update") == 0)
        {
            return "PUT";
        }
        else
            if (methodName.search(/Create/i) == 0 || methodName.search(/Insert/i) == 0)
            {
                return "POST";
            }
            else
                if (methodName.search(/Delete/i) == 0 || methodName.search(/Remove/i) == 0 || methodName.search(/Drop/i) == 0)
                {
                    return "DELETE";
                }
                else
                {
                    return "GET";
                }
}

function isExistingUriPattern(methodName, namespace, httpType, uriPattern)
{
    var restMapper = getRESTMapperByMethodInfo(methodName, namespace);
    var matchedWsOperations = restMapper.getWSOperationsByUriPatternAndHttpType(uriPattern, httpType);
    var originalWsOperation = getWSOperationByMethodInfo(methodName, namespace, httpType);
    for ( var i in matchedWsOperations)
    {
        if (originalWsOperation != matchedWsOperations[i])
        {
            return true;
        }
    }
    return false;
}


function getMappingTemplatefromRuntime(methodName, namespace) // Add web services to store the generated mapping xmls to xmlstore
{
    return getRESTMapperByMethodInfo(methodName, namespace).getXML();
}

function associateResource(methodName, resourceName)
{
    gResources[methodName] = resourceName;
}

function getSuggestedResource(methodName)
{
    var soapResponse = extractSOAPfromWSDL("response");
    var oldNewChildren = cordys.selectXMLNodes(soapResponse, "*[local-name()='tuple']/*[local-name()='new']/*" + "|*[local-name()='tuple']/*[local-name()='old']/*");
    var resource;
    if (oldNewChildren.length > 0)
    {
        resource = oldNewChildren[0].nodeName;
    }
    else
        if (soapResponse.firstChild && soapResponse.firstChild.nodeType == 1)
        {
            resource = soapResponse.firstChild.nodeName;
        }
        else
        {
            resource = soapResponse.nodeName;
        }
    if (resource.indexOf(":") != -1)
    {
        resource = resource.substr(resource.indexOf(":") + 1);
    }
    return resource.toLowerCase();
}

function getResource(methodName, namespace)
{
    if (!gResources[methodName])
    {
        var generatedResource = getSuggestedResource(methodName);
        associateResource(methodName, generatedResource);
    }
    return gResources[methodName];
}

function initResourceAssociations()
{
    gResources = new Object();
}

function getRESTMapperByMethodInfo(methodName, namespace)
{
    var resourceName = getResource(methodName, namespace);
    return gMappingManager.getRESTMapper(resourceName);
}

function getWSOperationByMethodInfo(methodName, namespace, httpType)
{
    var restMapper = getRESTMapperByMethodInfo(methodName, namespace);
    var wsOperation = restMapper.getWSOperation(methodName, namespace, httpType);
    if (!wsOperation)
    {
        throw new Error("REST Operation for the method " + methodName + " not found.");
    }
    return wsOperation;
}

function unsetRecentlyCreatedFlagForOperation(methodName, namespace, httpType)
{
    var wsOperation = getWSOperationByMethodInfo(methodName, namespace, httpType);
    wsOperation.setRecentlyCreated(false);
}
