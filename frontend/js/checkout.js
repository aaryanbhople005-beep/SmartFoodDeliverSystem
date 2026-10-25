/* SmartFoodDeliverySystem — checkout */
import { apiPost, loadCart, renderCartBadge, formatMoney, ensureSession, cart, toast } from "./app.js";

document.addEventListener("DOMContentLoaded", async () => {
    await loadCart();
    renderCartBadge();
    renderSummary();

    const form = document.getElementById("checkout-form");
    form.onsubmit = async (e) => {
        e.preventDefault();
        const customerName = document.getElementById("customer-name").value.trim();
        const orderType = document.querySelector('input[name="orderType"]:checked').value;
        const paymentMethod = document.getElementById("payment-method").value;

        if (!customerName) { toast("Please enter your name", "error"); return; }

        // Validate cart against backend (source of truth)
        if (!cart.items || cart.items.length === 0) {
            toast("Cart is empty. Add items before checkout.", "error");
            return;
        }

        const btn = form.querySelector("button[type='submit']");
        btn.disabled = true;
        btn.textContent = "Placing order...";

        try {
            const created = await apiPost("/orders", {
                session: ensureSession(),
                orderType: orderType,
                customerName: customerName
            });
            if (!created.success) { toast(created.error || "Order failed", "error"); btn.disabled = false; btn.textContent = "Place Order"; return; }

            const orderId = created.data.orderId;

            // Process payment via Strategy + Proxy
            const pay = await apiPost("/orders/" + orderId + "/pay", { action: "pay", method: paymentMethod });

            location.href = "order-confirmation.html?id=" + orderId +
                "&method=" + encodeURIComponent(paymentMethod) +
                "&success=" + (pay.success ? "true" : "false");
        } catch (err) {
            toast(err.message || "Checkout failed", "error");
            btn.disabled = false;
            btn.textContent = "Place Order";
        }
    };
});

function renderSummary() {
    const container = document.getElementById("order-items");
    const totalEl = document.getElementById("order-total");
    const items = cart.items || [];
    if (items.length === 0) {
        container.innerHTML = '<p class="empty">Your cart is empty.</p>';
        totalEl.textContent = formatMoney(0);
        return;
    }
    container.innerHTML = items.map(i => `
        <div class="order-item">
            <span>${escape(i.name)} <span class="qty">x${i.quantity}</span></span>
            <span>${formatMoney(i.subtotal)}</span>
        </div>
    `).join("");
    totalEl.textContent = formatMoney(cart.total);
}

function escape(s) { return String(s == null ? "" : s).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
