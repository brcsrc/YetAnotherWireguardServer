import { NavigateFunction } from "react-router";
import { FlashbarItem } from "../context/FlashbarContextProvider";

/**
 * Handles authentication errors (401/403) by adding a flashbar notification
 * and redirecting to the login page.
 *
 * This should be called in catch blocks when making authenticated API calls.
 */
export const handleAuthError = (
    error: any,
    navigate: NavigateFunction,
    addFlashbarItem: (item: FlashbarItem) => string
): boolean => {
    const status = error?.response?.status;

    if (status === 401 || status === 403) {
        addFlashbarItem({
            type: "error",
            header: "Session Expired",
            content: "Your session has expired. Please log in again.",
            dismissLabel: "Dismiss",
            duration: 5000
        });

        navigate("/login");
        return true;
    }

    return false;
};
