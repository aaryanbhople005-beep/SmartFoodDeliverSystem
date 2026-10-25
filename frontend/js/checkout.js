/* SmartFoodDeliverySystem — checkout */
import { apiPost, loadCart, renderCartBadge, formatMoney, ensureSession, cart, toast, getSession, loadCurrentUser } from "./app.js";

document.addEventListener("DOMContentLoaded", async () => {
    await loadCurrentUser();
    await loadCart();
    renderCartBadge();
    renderSummary();

    const form = document.getElementById("checkout-form");
    form.onsubmit = async (e) => {
        e.preventDefault();
        const customerName = document.getElementById("customer-name").value.trim();
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
            // Backend validates the cart and derives each item's order type from
            // its actual product category (no client-supplied order type).
            const created = await apiPost("/orders", {
                customerName: customerName
            });
            if (!created.success) { toast(created.error || "Order failed", "error"); btn.disabled = false; btn.textContent = "Place Order"; return; }

            const orders = created.data.orders || [];
            const total = created.data.total || 0;
            if (orders.length === 0) { toast("No orders were created.", "error"); btn.disabled = false; btn.textContent = "Place Order"; return; }

            // Process a payment for EVERY created order (Strategy + Proxy).
            // Do not show success unless every payment succeeds.
            const paymentResults = [];
            for (const o of orders) {
                const pay = await apiPost("/orders/" + o.orderId + "/pay", { method: paymentMethod });
                paymentResults.push({ orderId: o.orderId, success: pay.success, message: pay.error || pay.message });
                if (!pay.success) {
                    // Stop processing further payments; do not show success.
                    break;
                }
            }

            const allPaid = paymentResults.every(p => p.success);
            if (!allPaid) {
                const failed = paymentResults.find(p => !p.success);
                toast("Payment failed for order #" + failed.orderId + ": " + (failed.message || "unknown"), "error");
                btn.disabled = false;
                btn.textContent = "Place Order";
                return;
            }

            // Redirect to confirmation showing every order in the checkout.
            const ids = orders.map(o => o.orderId).join(",");
            location.href = "order-confirmation.html?id=" + orders[0].orderId +
                "&ids=" + encodeURIComponent(ids) +
                "&method=" + encodeURIComponent(paymentMethod) +
                "&success=true&total=" + encodeURIComponent(formatMoney(total));
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
