/* SmartFoodDeliverySystem — shared app logic (cart, nav, rendering) */
import { getSession, ensureSession, toast } from "./api.js";

let cart = { items: [], total: 0 };

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
        // cart may be empty on first visit; silently ignore
        cart = { items: [], total: 0 };
        renderCartBadge();
    }
}

/**
 * Add an item to the cart via the backend API, then sync local state.
 */
async function addToCart(item) {
    if (!item || !item.itemId || !item.name) {
        throw new Error("Invalid item: missing id or name.");
    }
    if (item.price < 0 || item.quantity <= 0) {
        throw new Error("Invalid price or quantity.");
    }
    await apiPost("/cart/add", {
        session: ensureSession(),
        itemId: item.itemId,
        name: item.name,
        price: item.price,
        quantity: item.quantity,
        category: item.category || "VEG",
        restaurant: item.restaurant || ""
    });
    // Sync local cart from backend after successful add
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

function removeFromCart(itemId) {
    cart.items = cart.items.filter(i => i.itemId !== itemId);
    recomputeTotal();
    renderCartBadge();
}

function recomputeTotal() {
    cart.total = (cart.items || []).reduce((s, i) => s + (i.subtotal || (i.price * i.quantity)), 0);
}

export {
    getSession, ensureSession, formatMoney, cart, loadCart, renderCartBadge,
    addToCart, updateQuantity, removeFromCart, recomputeTotal, apiGet, apiPost, toast
};
