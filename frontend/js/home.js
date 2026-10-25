/* SmartFoodDeliverySystem — home page */
import { loadCart, renderCartBadge, renderAuthNav, loadCurrentUser, addToCart, formatMoney, ensureSession, toast, apiGet } from "./app.js";

document.addEventListener("DOMContentLoaded", async () => {
    await loadCurrentUser();
    await loadCart();
    renderCartBadge();
    renderAuthNav();

    const grid = document.getElementById("featured-grid");
    if (!grid) return;

    try {
        // Load featured foods from the backend (Actual restaurants + factory menus),
        // not from a duplicated hardcoded list.
        const restData = await apiGet("/restaurants");
        const restaurants = (restData && Array.isArray(restData.data))
        ? restData.data
        : [];

        const featured = [];
        for (const r of restaurants) {
            const id = r.id;
            // Pick one Veg and one Combo item from each restaurant's real menu.
            for (const category of ["VEG", "COMBO"]) {
                const menuData = await apiGet("/restaurants/" + id + "/menu", { category: category });
                const items = (menuData && menuData.data && Array.isArray(menuData.data.items))
                ? menuData.data.items
                : [];
                if (items.length > 0) {
                    featured.push({ ...items[0], restaurant: r.name });
                }
            }
        }

        if (featured.length === 0) {
            grid.innerHTML = '<div class="empty-state"><div class="icon">🚫</div><p>No featured items available.</p></div>';
            return;
        }

        grid.innerHTML = featured.map(f => `            <article class="food-card">
                <div class="card-image">🍽️</div>
                <div class="card-title">${escape(f.name)}</div>
                <div class="card-sub">${escape(f.category)} &bull; ${escape(f.restaurant)}</div>
                <div class="card-footer">
                    <span class="price">${formatMoney(f.price)}</span>
                    <button class="btn btn-sm btn-primary add-btn" data-id="${escape(f.itemId)}">Add</button>
                </div>
            </article>
        `).join("");

        grid.addEventListener("click", async (e) => {
            const btn = e.target.closest(".add-btn");
            if (!btn) return;
            const itemId = btn.dataset.id;
            try {
                await addToCart({ itemId: itemId, quantity: 1 });
                toast("Added to cart", "success");
            } catch (err) {
                toast(err.message || "Failed to add item", "error");
            }
        });
    } catch (err) {
        grid.innerHTML = '<div class="empty-state"><div class="icon">⚠️</div><p>Unable to load featured items. Is the backend running?</p></div>';
        toast(err.message || "Failed to load featured items", "error");
    }
});

function escape(s) { return String(s == null ? "" : s).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
