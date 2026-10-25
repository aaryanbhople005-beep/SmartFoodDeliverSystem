/* SmartFoodDeliverySystem — shared app logic (cart, auth, nav, rendering) */
import {
    getSession,
    ensureSession,
    setSession,
    clearSession,
    toast,
    apiGet,
    apiPost
} from "./api.js";
import { initTheme } from "./theme.js";

let cart = { items: [], total: 0 };
let currentUser = null;

function formatMoney(n) {
    return "Rs." + Number(n || 0).toFixed(2);
}

function renderCartBadge() {
    const badge = document.getElementById("cart-badge");
    const qty = (cart.items || []).reduce((s, i) => s + (i.quantity || 0), 0);
    if (badge) {
        badge.textContent = qty;
        badge.style.display = qty > 0 ? "inline-block" : "none";
    }
}

function renderAuthNav() {
    const userNameEl = document.getElementById("nav-user-name");
    const guestLinks = document.getElementById("nav-guest-links");
    const userLinks = document.getElementById("nav-user-links");
    const cartLink = document.getElementById("nav-cart-link");
    if (!userNameEl && !guestLinks && !userLinks && !cartLink) return;
    if (currentUser) {
        if (userNameEl) userNameEl.textContent = currentUser.name || currentUser.email;
        if (guestLinks) guestLinks.style.display = "none";
        if (userLinks) userLinks.style.display = "flex";
        if (cartLink) cartLink.style.display = "inline-flex";
    } else {
        if (userNameEl) userNameEl.textContent = "";
        if (guestLinks) guestLinks.style.display = "flex";
        if (userLinks) userLinks.style.display = "none";
        if (cartLink) cartLink.style.display = "none";
    }
}

async function loadCurrentUser() {
    try {
        const data = await apiGet("/auth/me");
        if (data && data.success && data.data) {
            currentUser = data.data;
            renderAuthNav();
        } else {
            currentUser = null;
            renderAuthNav();
        }
    } catch (e) {
        currentUser = null;
        renderAuthNav();
    }
}

async function login(email, password) {
    const data = await apiPost("/auth/login", { email, password });
    if (data && data.success && data.data) {
        setSession(data.data.session);
        currentUser = data.data.user;
        renderAuthNav();
    }
    return data;
}

async function register(name, email, password) {
    const data = await apiPost("/auth/register", { name, email, password });
    return data;
}

async function logout() {
    try {
        await apiPost("/auth/logout", {});
    } catch (e) { /* ignore */ }
    clearSession();
    currentUser = null;
    renderAuthNav();
    location.href = "index.html";
}

function setupLogout() {
    const btn = document.getElementById("nav-logout");
    if (btn) {
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            logout();
        });
    }
}

/**
 * Load cart state from the backend (source of truth) and sync the local copy.
 */
async function loadCart() {
    try {
        ensureSession();
        const data = await apiGet("/cart", { session: getSession() });
        if (data && data.success && data.data) {
            cart = data.data;
            renderCartBadge();
        } else {
            cart = { items: [], total: 0 };
            renderCartBadge();
        }
    } catch (e) {
        cart = { items: [], total: 0 };
        renderCartBadge();
    }
}

/**
 * Add an item to the cart via the backend API. The browser sends only the item
 * id and quantity; the backend resolves the canonical name, price, category
 * and restaurant from its own menu data.
 * Requires user to be logged in. If not logged in, redirects to sign-in page
 * with a message and preserves the current page as redirect target.
 */
async function addToCart(item) {
    if (!item || !item.itemId) {
        throw new Error("Invalid item: missing id.");
    }
    if (!item.quantity || item.quantity <= 0) {
        throw new Error("Invalid quantity.");
    }

    // Check if user is authenticated
    if (!currentUser) {
        // Store current page as redirect target
        const currentPath = window.location.pathname + window.location.search;
        const loginUrl = "sign-in.html?redirect=" + encodeURIComponent(currentPath) + "&message=" + encodeURIComponent("Please sign in to order food.");
        window.location.href = loginUrl;
        return;
    }

    await apiPost("/cart/add", {
        itemId: item.itemId,
        quantity: item.quantity
    });
    await loadCart();
    renderCartBadge();
}

function updateQuantity(itemId, quantity) {
    const item = cart.items.find(i => i.itemId === itemId);
    if (!item) return;
    if (quantity <= 0) {
        removeFromCart(itemId);
        return;
    }
    item.quantity = quantity;
    recomputeTotal();
    renderCartBadge();
}

async function removeFromCart(itemId) {
    try {
        await apiPost("/cart/remove", { itemId });
    } catch (e) {
        toast(e.message || "Failed to remove item", "error");
    }
    cart.items = cart.items.filter(i => i.itemId !== itemId);
    recomputeTotal();
    renderCartBadge();
}

function recomputeTotal() {
    cart.total = (cart.items || []).reduce((s, i) => s + (i.subtotal || (i.price * i.quantity)), 0);
}

export {
    getSession, ensureSession, setSession, clearSession, formatMoney, cart, loadCart, renderCartBadge,
    renderAuthNav, loadCurrentUser, login, register, logout, currentUser,
    addToCart, updateQuantity, removeFromCart, recomputeTotal, apiGet, apiPost, toast,
    setupLogout, initTheme
};
