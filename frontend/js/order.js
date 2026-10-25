/* SmartFoodDeliverySystem — order confirmation & status */
import { apiGet, apiPost, loadCart, renderCartBadge, formatMoney, ensureSession, toast } from "./app.js";

const urlParams = new URLSearchParams(location.search);
const orderId = parseInt(urlParams.get("id"));

document.addEventListener("DOMContentLoaded", async () => {
    await loadCart();
    renderCartBadge();

    if (document.getElementById("order-number")) {
        initConfirmation();
    } else if (document.getElementById("status-pill")) {
        initStatus();
    }
});

function parseInt(s) { return s == null ? -1 : Number(s); }

async function initConfirmation() {
    try {
        const data = await apiGet("/orders/" + orderId);
        if (data && data.success) {
            const d = data.data;
            document.getElementById("order-number").textContent = "#" + d.orderId;
            document.getElementById("order-customer").textContent = d.customerName;
            document.getElementById("order-restaurant").textContent = d.restaurantName;
            document.getElementById("order-type").textContent = d.orderType;
            document.getElementById("order-item").textContent = d.itemName;
            document.getElementById("order-qty").textContent = d.quantity;
            document.getElementById("order-item-price").textContent = formatMoney(d.itemPrice);
            document.getElementById("order-total").textContent = formatMoney(d.amount);
            document.getElementById("order-status").textContent = "PLACED";
            const method = urlParams.get("method") || "COD";
            const success = urlParams.get("success") === "true";
            document.getElementById("order-payment").textContent = method + (success ? " (Paid)" : " (Pay on Delivery)");
        }
    } catch (e) {
        toast(e.message || "Failed to load order", "error");
    }

    document.getElementById("reorder-btn").onclick = async () => {
        try {
            const data = await apiPost("/orders/" + orderId + "/reorder", { action: "reorder" });
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
    await refreshStatus();
    setInterval(refreshStatus, 5000);
    document.getElementById("advance-btn").onclick = advanceStatus;
    document.getElementById("back-home2").onclick = () => location.href = "index.html";
}

async function refreshStatus() {
    try {
        const data = await apiGet("/orders/" + orderId + "/status");
        if (data && data.success) {
            const status = data.data.status;
            const pill = document.getElementById("status-pill");
            pill.textContent = status;
            pill.className = "status-pill status-" + status.toLowerCase();
            document.getElementById("status-time").textContent = new Date().toLocaleTimeString();
            document.getElementById("advance-btn").disabled =
                status === "DELIVERED" || status === "CANCELLED";
        }
    } catch (e) {
        // ignore polling errors
    }
}

async function advanceStatus() {
    try {
        const data = await apiPost("/orders/" + orderId + "/advance-status", { action: "advance-status" });
        if (data && data.success) {
            toast("Status: " + data.data.status, "success");
            refreshStatus();
        }
    } catch (e) {
        toast(e.message || "Failed to advance status", "error");
    }
}
