var responseText = "";

function RestfulResource(resource_url)
{
    this.resource_url = system.addAuthentication(resource_url);
    this.xmlhttp = cordys.getConnection();

    this.onRetrieveSuccess = function(responseText)
    {
        ipStatus.setValue(this.xmlhttp.status);
        taResponse.setValue(responseText);
    };
    
    this.onRetrieveError = function(statusText)
    {
        ipStatus.setValue(this.xmlhttp.status);
        taResponse.setValue(statusText);
    };
    
    this.onCreateSuccess = function(responseText)
    {
        ipStatus.setValue(this.xmlhttp.status);
        taResponse.setValue(responseText);
    };
    
    this.onCreateError = function(statusText)
    {
        ipStatus.setValue(this.xmlhttp.status);
        taResponse.setValue(statusText);
    };
    
    this.onUpdateSuccess = function(responseText)
    {
        ipStatus.setValue(this.xmlhttp.status);
        taResponse.setValue(responseText);
    };
    
    this.onUpdateError = function(statusText)
    {
        ipStatus.setValue(this.xmlhttp.status);
        taResponse.setValue(statusText);
    };
    
    this.onRemoveSuccess = function(responseText)
    {
        ipStatus.setValue(this.xmlhttp.status);
        taResponse.setValue(responseText);
    };
    
    this.onRemoveError = function(statusText)
    {
        ipStatus.setValue(this.xmlhttp.status);
        taResponse.setValue(statusText);
    };
}

/**
 * Get the resource or a list of resources calling the RESTful web service with the GET http method
 */
RestfulResource.prototype.retrieve = function()
{
    var url = this.resource_url;
    var self = this;
    this.xmlhttp.onreadystatechange = function()
    {
        if (self.xmlhttp.readyState == 4)
        {
            if (self.xmlhttp.status == 200)
            {
                self.onRetrieveSuccess.call(self, self.xmlhttp.responseText);
            }
            else
            {
                self.onRetrieveError.call(self, self.xmlhttp.responseText);
            }
        }
    };
    this.xmlhttp.open("GET", url, true);
    this.xmlhttp.send(null);
};

/**
 * Update a resource calling the RESTful web service with the PUT http method
 */
RestfulResource.prototype.update = function(xmlString)
{
    var self = this;
    this.xmlhttp.onreadystatechange = function()
    {
        if (self.xmlhttp.readyState == 4)
        {
            if (self.xmlhttp.status == 200)
            {
                self.onUpdateSuccess.call(self, self.xmlhttp.responseText);
            }
            else
            {
                self.onUpdateError.call(self, self.xmlhttp.responseText);
            }
        }
    };
    this.xmlhttp.open("POST", this.resource_url, true);
    this.xmlhttp.setRequestHeader("Content-type", "application/xml");
    this.xmlhttp.setRequestHeader("Content-length", xmlString.length);
    // this.xmlhttp.setRequestHeader("Connection", "close");
    this.xmlhttp.send(xmlString);
};

/**
 * Create the resource calling the RESTful web service with the POST http method
 */
RestfulResource.prototype.create = function(xmlString)
{
    var self = this;
    this.xmlhttp.onreadystatechange = function()
    {
        if (self.xmlhttp.readyState == 4)
        {
            if (self.xmlhttp.status == 200)
            {
                self.onCreateSuccess.call(self, self.xmlhttp.responseText);
            }
            else
            {
                self.onCreateError.call(self, self.xmlhttp.responseText);
            }
        }
    };
    this.xmlhttp.open("PUT", this.resource_url, true);
    this.xmlhttp.setRequestHeader("Content-type", "application/xml");
    this.xmlhttp.setRequestHeader("Content-length", xmlString.length);
    // this.xmlhttp.setRequestHeader("Connection", "close");
    this.xmlhttp.send(xmlString);
};

/**
 * Remove a resource calling the RESTful web service with the DELETE http method
 */
RestfulResource.prototype.remove = function()
{
    var url = this.resource_url;
    var self = this;
    this.xmlhttp.onreadystatechange = function()
    {
        if (self.xmlhttp.readyState == 4)
        {
            if (self.xmlhttp.status == 200)
            {
                self.onRemoveSuccess.call(self, self.xmlhttp.responseText);
            }
            else
            {
                self.onRemoveError.call(self, self.xmlhttp.responseText);
            }
        }
    };
    this.xmlhttp.open("DELETE", url, true);
    this.xmlhttp.send(null);
};