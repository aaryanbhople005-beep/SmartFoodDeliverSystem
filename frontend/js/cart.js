/* SmartFoodDeliverySystem — cart page */
import { apiGet, apiPost, loadCart, renderCartBadge, formatMoney, ensureSession, cart, recomputeTotal, toast, loadCurrentUser, currentUser } from "./app.js";

document.addEventListener("DOMContentLoaded", async () => {
    await loadCurrentUser();
    await loadCart();
    renderCart();
    document.getElementById("continue-shopping").onclick = () => location.href = "restaurants.html";
    document.getElementById("checkout").onclick = () => location.href = "checkout.html";
    document.getElementById("save-cart").onclick = saveCart;
    document.getElementById("restore-cart").onclick = restoreCart;
});

function renderCart() {
    const container = document.getElementById("cart-items");
    const empty = document.getElementById("cart-empty");
    const totalEl = document.getElementById("cart-total");
    const items = cart.items || [];

    if (items.length === 0) {
        container.innerHTML = "";
        empty.style.display = "block";
        totalEl.textContent = formatMoney(0);
        document.getElementById("checkout").disabled = true;
        return;
    }
    empty.style.display = "none";
    document.getElementById("checkout").disabled = false;

    container.innerHTML = items.map(item => `
        <div class="cart-item">
            <div class="image">🍽️</div>
            <div class="info">
                <div class="name">${escape(item.name)}</div>
                <div style="font-size:0.85rem;color:var(--text-muted);">${escape(item.restaurant)}</div>
                <div class="price">${formatMoney(item.price)}</div>
            </div>
            <div class="cart-controls">
                <button class="qty-btn" data-id="${escape(item.itemId)}" data-dir="-">−</button>
                <span class="qty">${item.quantity}</span>
                <button class="qty-btn" data-id="${escape(item.itemId)}" data-dir="+">+</button>
            </div>
            <button class="remove-btn" data-id="${escape(item.itemId)}">Remove</button>
        </div>
    `).join("");

    container.querySelectorAll(".qty-btn").forEach(btn => {
        btn.onclick = () => changeQty(btn.dataset.id, btn.dataset.dir === "+" ? 1 : -1);
    });
    container.querySelectorAll(".remove-btn").forEach(btn => {
        btn.onclick = () => removeItem(btn.dataset.id);
    });

    totalEl.textContent = formatMoney(cart.total);
}

async function changeQty(itemId, dir) {
    const item = cart.items.find(i => i.itemId === itemId);
    if (!item) return;
    const next = item.quantity + dir;
    try {
        await apiPost("/cart/quantity", {
            session: ensureSession(),
            itemId: item.itemId,
            quantity: next
        });
        cart.items = cart.items.map(i => i.itemId === itemId ? { ...i, quantity: next } : i);
        recomputeTotal();
        renderCart();
        renderCartBadge();
    } catch (e) {
        toast(e.message || "Failed to update quantity", "error");
    }
}

async function removeItem(itemId) {
    try {
        await apiPost("/cart/remove", { session: ensureSession(), itemId });
        cart.items = cart.items.filter(i => i.itemId !== itemId);
        recomputeTotal();
        renderCart();
        renderCartBadge();
    } catch (e) {
        toast(e.message || "Failed to remove item", "error");
    }
}

async function saveCart() {
    const btn = document.getElementById("save-cart");
    
    // Check if user is authenticated
    if (!currentUser) {
        toast("Please sign in to save your cart.", "error");
        setTimeout(() => location.href = "sign-in.html?redirect=" + encodeURIComponent(window.location.pathname), 1500);
        return;
    }
    
    // Check if cart is empty
    if (!cart.items || cart.items.length === 0) {
        toast("Your cart is empty. Add items before saving.", "error");
        return;
    }
    
    // Prevent double-click
    if (btn.disabled) return;
    btn.disabled = true;
    btn.textContent = "Saving...";
    
    try {
        const data = await apiPost("/cart/save", { session: ensureSession() });
        const token = data.data?.token || data.token;
        if (!token) {
            throw new Error("Invalid response from server");
        }
        document.getElementById("restore-token").value = token;
        document.getElementById("token-box").style.display = "block";
        toast("Cart snapshot saved (Memento)", "success");
    } catch (e) {
        toast(e.message || "Failed to save cart", "error");
    } finally {
        btn.disabled = false;
        btn.textContent = "Save Cart";
    }
}

async function restoreCart() {
    const token = document.getElementById("restore-token").value.trim();
    if (!token) { toast("Enter a restore token", "error"); return; }
    
    const btn = document.getElementById("restore-cart");
    if (btn.disabled) return;
    btn.disabled = true;
    btn.textContent = "Restoring...";
    
    try {
        await apiPost("/cart/restore", { session: ensureSession(), token });
        await loadCart();
        renderCart();
        renderCartBadge();
        toast("Cart restored (Memento)", "success");
    } catch (e) {
        toast(e.message || "Failed to restore cart", "error");
    } finally {
        btn.disabled = false;
        btn.textContent = "Restore Cart";
    }
}

function escape(s) { return String(s == null ? "" : s).replace(/&/g, "&").replace(/</g, "<").replace(/>/g, ">"); }