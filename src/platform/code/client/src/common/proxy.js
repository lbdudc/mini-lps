import properties from "@/properties";

/**
 * Transform an unsafe URL to a LVM's proxy URL
 */
function handleURL(url, useProxy = false) {
  const urlProtocol = new URL(url).protocol;
  if (!useProxy && urlProtocol.includes("https")) {
    return url;
  } else {
    const REQUEST_RESOURCE_START = properties.SERVER_URL + "/proxy/";
    const requestURL = btoa(encodeURIComponent(url));
    const REQUEST_RESOURCE_END = "/mapViewer";
    return REQUEST_RESOURCE_START + requestURL + REQUEST_RESOURCE_END;
  }
}

/**
 * Transform LVM's proxy URL to original URL
 */
function retrieveURL(url) {
  const REQUEST_RESOURCE_START = properties.SERVER_URL + "/proxy/";
  const REQUEST_RESOURCE_END = "/mapViewer";

  while (!Array.isArray(url) && url.startsWith(REQUEST_RESOURCE_START)) {
    let base64Encoded = url
      .replace(REQUEST_RESOURCE_START, "")
      .replace(REQUEST_RESOURCE_END, "");

    url = decodeURIComponent(atob(base64Encoded));
  }

  return url;
}

/**
 * Deals with HTTPs and HTTP requests
 */
async function handleRequest(url, optionsRequest = null) {
  try {
    const urlProtocol = new URL(url).protocol;
    // If HTTPS, perform the request directly
    if (urlProtocol.includes("https")) {
      // If options object is provided, include it in the request
      var result = await fetch(url, optionsRequest ? optionsRequest : {});
      return result;
    } else {
      // If HTTP, send the request through the backend
      return _httpResponse(url, optionsRequest);
    }
  } catch (error) {
    return _httpResponse(url, optionsRequest);
  }
}

async function _httpResponse(url, optionsRequest = null) {
  const REQUEST_RESOURCE = properties.SERVER_URL + "/proxy";

  const httpRequest = JSON.stringify({
    url: url,
    method:
      optionsRequest != null
        ? optionsRequest.method === undefined
          ? "GET"
          : optionsRequest.method
        : "GET",
    body:
      optionsRequest != null
        ? optionsRequest.body === undefined
          ? ""
          : optionsRequest.body
        : "",
    headers:
      optionsRequest != null
        ? optionsRequest.headers === undefined
          ? null
          : optionsRequest.headers
        : null,
  });

  const optionsFetch = {
    method: "POST",
    body: httpRequest,
    headers: {
      "X-APP-TOKEN": "Bearer " + localStorage.getItem("token"),
      "Content-Type": "application/json;charset=UTF-8",
    },
  };

  try {
    return await fetch(REQUEST_RESOURCE, optionsFetch);
  } catch (err) {
    throw err;
  }
}

export { handleURL, retrieveURL, handleRequest };
