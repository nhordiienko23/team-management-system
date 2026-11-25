export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}

export interface CsrfTokenResponse {
  token: string;
  parameterName: string;
  headerName: string;
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly error: string,
    readonly timestamp?: string,
    options?: ErrorOptions,
  ) {
    super(message, options);
    this.name = "ApiError";
  }
}

let csrfToken: CsrfTokenResponse | null = null;
let csrfTokenRequest: Promise<CsrfTokenResponse> | null = null;

export async function getCsrfToken(forceRefresh = false): Promise<CsrfTokenResponse> {
  if (!forceRefresh && csrfToken) {
    return csrfToken;
  }
  if (csrfTokenRequest) {
    return csrfTokenRequest;
  }
  if (forceRefresh) {
    csrfToken = null;
  }

  const request = (async () => {
    let response: Response;
    try {
      response = await fetch("/api/auth/csrf", {
        credentials: "same-origin",
        cache: "no-store",
        headers: { Accept: "application/json" },
      });
    } catch (cause) {
      throw new ApiError(
        0,
        "Unable to load the security token. Check your connection and try again.",
        "Network Error",
        undefined,
        { cause },
      );
    }

    if (!response.ok) {
      throw new ApiError(
        response.status,
        "Unable to load the security token. Please try again.",
        response.statusText,
      );
    }

    let payload: unknown;
    try {
      payload = await response.json();
    } catch (cause) {
      throw new ApiError(
        response.status,
        "The server returned an invalid security token response.",
        "Invalid Response",
        undefined,
        { cause },
      );
    }

    if (!isCsrfTokenResponse(payload)) {
      throw new ApiError(
        response.status,
        "The server returned an invalid security token response.",
        "Invalid Response",
      );
    }

    csrfToken = payload;
    return payload;
  })();

  csrfTokenRequest = request;
  try {
    return await request;
  } finally {
    if (csrfTokenRequest === request) {
      csrfTokenRequest = null;
    }
  }
}

export function clearCsrfToken(): void {
  csrfToken = null;
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event("csrf-token-invalidated"));
  }
}

export async function requestJson<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");

  if (typeof options.body === "string" && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const method = (options.method ?? "GET").toUpperCase();
  const sameOrigin = isSameOrigin(path);
  const safeMethods = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);
  if (sameOrigin && !safeMethods.has(method)) {
    const csrf = await getCsrfToken();
    headers.set(csrf.headerName, csrf.token);
  }

  let response: Response;
  try {
    response = await fetch(path, {
      ...options,
      headers,
      credentials: "same-origin",
    });
  } catch (cause) {
    throw new ApiError(
      0,
      "Unable to reach the server. Check your connection and try again.",
      "Network Error",
      undefined,
      { cause },
    );
  }

  const contentType = response.headers.get("content-type") ?? "";
  const isJson = contentType.includes("application/json");
  let payload: unknown;

  if (isJson) {
    try {
      payload = await response.json();
    } catch (cause) {
      throw new ApiError(
        response.status,
        "The server returned an invalid response.",
        "Invalid Response",
        undefined,
        { cause },
      );
    }
  } else if (response.status !== 204) {
    const message = response.ok
      ? "The server returned an unexpected response."
      : `The request failed with status ${response.status}.`;
    if (!response.ok) {
      throw new ApiError(response.status, message, response.statusText);
    }
    throw new ApiError(response.status, message, "Invalid Response");
  }

  if (!response.ok) {
    const errorResponse = isErrorResponse(payload) ? payload : undefined;
    if (response.status === 401 && typeof window !== "undefined") {
      window.dispatchEvent(new Event("session-expired"));
    }

    if (response.status === 403 && errorResponse?.error === "CSRF Failed") {
      clearCsrfToken();
      throw new ApiError(
        response.status,
        "Your security token is missing or expired. Refresh the page and try again.",
        errorResponse.error,
        errorResponse.timestamp,
      );
    }

    throw new ApiError(
      response.status,
      errorResponse?.message ?? `The request failed with status ${response.status}.`,
      errorResponse?.error ?? response.statusText,
      errorResponse?.timestamp,
    );
  }

  return payload as T;
}

export function postJson<T>(path: string, body: unknown): Promise<T> {
  return requestJson<T>(path, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

function isErrorResponse(value: unknown): value is ErrorResponse {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const response = value as Partial<ErrorResponse>;
  return (
    typeof response.status === "number" &&
    typeof response.error === "string" &&
    typeof response.message === "string" &&
    typeof response.timestamp === "string"
  );
}

function isCsrfTokenResponse(value: unknown): value is CsrfTokenResponse {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const response = value as Partial<CsrfTokenResponse>;
  return (
    typeof response.token === "string" &&
    response.token.length > 0 &&
    typeof response.parameterName === "string" &&
    response.parameterName.length > 0 &&
    typeof response.headerName === "string" &&
    response.headerName.length > 0
  );
}

function isSameOrigin(path: string): boolean {
  if (typeof window === "undefined") {
    return false;
  }

  try {
    return new URL(path, window.location.origin).origin === window.location.origin;
  } catch {
    return false;
  }
}
