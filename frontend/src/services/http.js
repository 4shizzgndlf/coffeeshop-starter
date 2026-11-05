// services/http.js
// Use relative URLs like "/api/products" for your own backend.
// Every request goes through request(), so error handling (and later the login
// token, when Spring Security is added) only has to be written once.

// An error the user can be shown, e.g. "Only 2 left of Kenya Nyeri AA".
// - status: the HTTP status, or 0 if the server could not be reached
// - fieldErrors: { email: "must be a well-formed email address" } when validation failed (400)
export class ApiError extends Error {
    constructor(status, message, fieldErrors = {}) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}

// Fetches a URL and returns the parsed JSON. Throws an ApiError if the response isn't 2xx.
export function getJSON(url) {
    return request("GET", url);
}

// Sends a request with an optional JSON body, e.g. sendJSON("POST", "/api/carts").
// Returns the parsed JSON, or null when there's no body (204 No Content).
export function sendJSON(method, url, body) {
    return request(method, url, body);
}

async function request(method, url, body) {
    const options = {
        method,
        headers: { Accept: "application/json" }
    };

    if (body !== undefined) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
    }

    let response;

    try {
        response = await fetch(url, options);
    } catch (error) {
        throw new ApiError(0, "Cannot reach the server. Check your connection and try again.");
    }

    if (response.status === 204) {
        return null;
    }

    const data = await readJson(response);

    if (!response.ok) {
        throw new ApiError(response.status, errorMessage(response.status, data), data?.errors ?? {});
    }

    return data;
}

// Returns the parsed JSON body, or null if the body is empty or not JSON
async function readJson(response) {
    try {
        return await response.json();
    } catch (error) {
        return null;
    }
}

// Spring Boot sends errors as ProblemDetail: {"status": 409, "detail": "Only 2 left of ..."}.
// Anything else, such as an HTML error page when the API is down, gets a general message.
function errorMessage(status, data) {
    if (data?.detail) {
        return data.detail;
    }

    // Caddy answers 502 when it can't reach Spring Boot
    if (status === 502 || status === 503 || status === 504) {
        return `Cannot reach the API (HTTP ${status}). Is Spring Boot running?`;
    }

    if (status >= 500) {
        return `The server had a problem (HTTP ${status}). Try again in a moment.`;
    }

    return `The request failed (HTTP ${status}).`;
}
