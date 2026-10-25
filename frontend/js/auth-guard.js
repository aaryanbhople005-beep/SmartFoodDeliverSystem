/* SmartFoodDeliverySystem — Authentication Guard */
import { loadCurrentUser, currentUser } from "./app.js";

const PUBLIC_PATHS = [
    "/sign-in.html",
    "/sign-up.html",
    "/",
    "/index.html",
    "/restaurants.html",
    "/menu.html"
];

function getCurrentPath() {
    return window.location.pathname;
}

function isPublicPath(path) {
    return PUBLIC_PATHS.some(p => path === p || path.endsWith(p));
}

function getRedirectUrl() {
    const currentPath = getCurrentPath();
    if (isPublicPath(currentPath)) {
        return null;
    }
    return currentPath + window.location.search;
}

export async function requireAuth() {
    await loadCurrentUser();

    if (!currentUser) {
        const redirectUrl = getRedirectUrl();
        const loginUrl = redirectUrl
            ? "sign-in.html?redirect=" + encodeURIComponent(redirectUrl)
            : "sign-in.html";
        window.location.href = loginUrl;
        return false;
    }
    return true;
}

export function getRedirectAfterLogin() {
    const params = new URLSearchParams(window.location.search);
    return params.get("redirect") || "index.html";
}

export function clearRedirect() {
    const url = new URL(window.location.href);
    url.searchParams.delete("redirect");
    url.searchParams.delete("message");
    window.history.replaceState({}, document.title, url.toString());
}

export function getRedirectMessage() {
    const params = new URLSearchParams(window.location.search);
    return params.get("message") || null;
}