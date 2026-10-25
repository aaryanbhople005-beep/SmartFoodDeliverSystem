/* SmartFoodDeliverySystem — order confirmation & status */
import { apiGet, apiPost, loadCart, renderCartBadge, formatMoney, ensureSession, toast, loadCurrentUser, currentUser, logout } from "./app.js";

const urlParams = new URLSearchParams(location.search);
const orderId = parseInt(urlParams.get("id"));
const idsParam = urlParams.get("ids");
const orderIds = idsParam ? idsParam.split(",").map(s => parseInt(s)).filter(s => !isNaN(s)) : (orderId >= 0 ? [orderId] : []);

document.addEventListener("DOMContentLoaded", async () => {
    await loadCurrentUser();
    await loadCart();
    renderCartBadge();

    if (!currentUser) {
        // Order pages require authentication.
        toast("Please sign in to view your order.", "error");
        if (document.getElementById("order-number") || document.getElementById("status-pill")) {
            // Allow a brief toast then keep on page; protected endpoints will 401.
        }
    }

    if (document.getElementById("order-number")) {
        initConfirmation();
    } else if (document.getElementById("status-pill")) {
        initStatus();
    }
});

function parseInt(s) { return s == null ? -1 : Number(s); }

async function initConfirmation() {
    if (orderIds.length === 0) {
        toast("No order specified.", "error");
        return;
    }
    try {
        const primary = await apiGet("/orders/" + orderId);
        if (!(primary && primary.success)) throw new Error(primary && primary.error ? primary.error : "Failed to load order");

        const d = primary.data;
        document.getElementById("order-number").textContent = "#" + d.orderId;
        document.getElementById("order-customer").textContent = d.customerName;
        document.getElementById("order-restaurant").textContent = d.restaurantName;

        // Load every order in this checkout and render all items.
        const containers = [];
        const itemsContainer = document.getElementById("order-items-list");
        let grandTotal = 0;
        let paymentMethod = urlParams.get("method") || "COD";

        for (const oid of orderIds) {
            const data = await apiGet("/orders/" + oid);
            if (!(data && data.success)) continue;
            const od = data.data;
            grandTotal += Number(od.amount || 0);
            const el = document.createElement("div");
            el.className = "order-item";
            el.innerHTML = `
                <span><strong>${escape(od.itemName)}</strong> <span class="qty">x${od.quantity}</span> (${escape(od.orderType)})</span>
                <span>${formatMoney(od.itemPrice)} each</span>
                <span>${formatMoney(od.amount)}</span>
            `;
            if (itemsContainer) itemsContainer.appendChild(el);
        }

        if (itemsContainer) itemsContainer.style.display = orderIds.length > 0 ? "block" : "none";

        // Single-item fallback (legacy): show primary order fields too.
        document.getElementById("order-type").textContent = d.orderType;
        document.getElementById("order-item").textContent = d.itemName;
        document.getElementById("order-qty").textContent = d.quantity;
        document.getElementById("order-item-price").textContent = formatMoney(d.itemPrice);
        document.getElementById("order-total").textContent = formatMoney(grandTotal > 0 ? grandTotal : d.amount);
        document.getElementById("order-status").textContent = "PLACED";
        const success = urlParams.get("success") === "true";
        document.getElementById("order-payment").textContent = paymentMethod + (success ? " (Paid)" : " (Pay on Delivery)");

        if (document.getElementById("grand-total-row")) {
            document.getElementById("grand-total-row").style.display = orderIds.length > 1 ? "block" : "none";
            if (document.getElementById("grand-total")) document.getElementById("grand-total").textContent = formatMoney(grandTotal);
        }
    } catch (e) {
        toast(e.message || "Failed to load order", "error");
    }

    document.getElementById("reorder-btn").onclick = async () => {
        if (!currentUser) { toast("Please sign in to reorder.", "error"); return; }
        try {
            const data = await apiPost("/orders/" + orderId + "/reorder", {});
            if (data && data.success) {
                toast("Order copied via Prototype. Review below.", "success");
                document.getElementById("reorder-preview").style.display = "block";
                const p = data.data;
                document.getElementById("copy-item").textContent = p.itemName;
                document.getElementById("copy-qty").textContent = p.quantity;
                document.getElementById("copy-amount").textContent = formatMoney(p.amount);
            }
        } catch (e) {
            toast(e.message || "Reorder failed", "error");
        }
    };

    document.getElementById("back-home").onclick = () => location.href = "index.html";
}

async function initStatus() {
    const singleId = orderIds.length > 0 ? orderIds[0] : orderId;
    if (singleId < 0) return;
    await refreshStatus(singleId);
    setInterval(() => refreshStatus(singleId), 5000);
    document.getElementById("advance-btn").onclick = async () => {
        if (!currentUser) { toast("Please sign in.", "error"); return; }
        try {
            const data = await apiPost("/orders/" + singleId + "/advance-status", {});
            if (data && data.success) {
                toast("Status: " + data.data.status, "success");
                refreshStatus(singleId);
            }
        } catch (e) {
            toast(e.message || "Failed to advance status", "error");
        }
    };
    document.getElementById("back-home2").onclick = () => location.href = "index.html";
}

async function refreshStatus(id) {
    try {
        const data = await apiGet("/orders/" + id + "/status");
        if (data && data.success) {
            const status = data.data.status;
            const pill = document.getElementById("status-pill");
            if (pill) {
                pill.textContent = status;
                pill.className = "status-pill status-" + status.toLowerCase();
            }
            const timeEl = document.getElementById("status-time");
            if (timeEl) timeEl.textContent = new Date().toLocaleTimeString();
            const btn = document.getElementById("advance-btn");
            if (btn) btn.disabled = status === "DELIVERED" || status === "CANCELLED";
        }
    } catch (e) {
        // ignore polling errors
    }
}
