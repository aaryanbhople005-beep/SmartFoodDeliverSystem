/* SmartFoodDeliverySystem — restaurant listing */
import { apiGet, loadCart, renderCartBadge, formatMoney, ensureSession, loadCurrentUser, toast } from "./app.js";

document.addEventListener("DOMContentLoaded", async () => {
    await loadCurrentUser();
    await loadCart();
    renderCartBadge();
    loadRestaurants();
});

async function loadRestaurants() {
    const grid = document.getElementById("restaurant-grid");
    if (!grid) return;
    try {
        const data = await apiGet("/restaurants");
        if (data && Array.isArray(data.data)) {
            grid.innerHTML = data.data.map(r => `
                <article class="restaurant-card">
                    <div class="card-image">🏪</div>
                    <div class="card-title">${escape(r.name)}</div>
                    <div class="card-sub">${escape(r.category)}</div>
                    <p style="font-size:0.9rem;color:var(--text-muted);margin-bottom:12px;">${escape(r.description)}</p>
                    <div class="card-footer">
                        <span class="price">View Menu</span>
                        <button class="btn btn-sm btn-primary" onclick="location.href='menu.html?id=${escape(r.id)}'">Menu</button>
                    </div>
                </article>
            `).join("");
        }
    } catch (e) {
        document.getElementById("restaurant-grid").innerHTML =
            '<div class="empty-state"><div class="icon">⚠️</div><p>Unable to load restaurants. Is the backend running?</p></div>';
        toast(e.message || "Failed to load restaurants", "error");
    }
}

function escape(s) { return String(s == null ? "" : s).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
