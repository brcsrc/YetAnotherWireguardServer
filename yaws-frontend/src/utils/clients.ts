import {
    Configuration,
    UserControllerApi,
    NetworkControllerApi,
    NetworkClientControllerApi,
    ToolControllerApi,
    ResponseContext,
    Middleware,
} from "@yaws/yaws-ts-api-client";

/**
 * creating instances of each client here once to be reused throughout
 */

/**
 * Middleware to attach response data to errors for better error messages.
 *
 * OpenAPI Generator's typescript-fetch has a known limitation where middleware.onError
 * doesn't receive HTTP error responses (401, 403, 500, etc.). The onError handler only
 * fires for network failures, not HTTP error status codes.
 *
 * Workaround: Use the 'post' middleware hook to intercept error responses before
 * the generator's validation throws an exception. This allows us to attach the
 * response body to the error object for better error messages in the UI.
 *
 * Reference: https://github.com/OpenAPITools/openapi-generator/issues/17979
 */
const errorMiddleware: Middleware = {
    post: async (context: ResponseContext): Promise<Response | void> => {
        if (!context.response.ok) {
            const body = await context.response.clone().json().catch(() => ({}));
            const error: any = new Error(`HTTP ${context.response.status}: ${context.response.statusText}`);
            error.response = {
                status: context.response.status,
                statusText: context.response.statusText,
                data: body
            };
            throw error;
        }
        return context.response;
    }
};

const config = new Configuration({
    credentials: "include", // Include cookies in all requests
    middleware: [errorMiddleware],
});

export const userClient = new UserControllerApi(config);
export const networkClient = new NetworkControllerApi(config);
export const networkClientClient = new NetworkClientControllerApi(config);
export const toolsClient = new ToolControllerApi(config);
