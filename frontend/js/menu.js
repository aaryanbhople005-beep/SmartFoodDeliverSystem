/* SmartFoodDeliverySystem — restaurant menu */
import {
    apiGet,
    loadCart,
    renderCartBadge,
    formatMoney,
    ensureSession,
    apiPost,
    cart,
    recomputeTotal,
    loadCurrentUser,
    addToCart,
    toast
} from "./app.js";

const urlParams = new URLSearchParams(location.search);
const restaurantId = urlParams.get("id");

document.addEventListener("DOMContentLoaded", async () => {
    await loadCurrentUser();
    await loadCart();
    renderCartBadge();
    if (!restaurantId) {
        document.getElementById("restaurant-name").textContent = "Unknown Restaurant";
        return;
    }
    loadRestaurant(restaurantId);
    loadMenu(restaurantId, "VEG");

    document.getElementById("category-tabs").addEventListener("click", (e) => {
        const btn = e.target.closest(".cat-btn");
        if (!btn) return;

        document.querySelectorAll(".cat-btn").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");

        loadMenu(restaurantId, btn.dataset.cat);
    });

    document.getElementById("menu-grid").addEventListener("click", async (ev) => {
        const btn = ev.target.closest(".add-btn");
        if (!btn) return;

        try {
            await addToCart({
                itemId: btn.dataset.id,
                quantity: 1
            });

            toast("Added to cart", "success");
        } catch (err) {
            toast(err.message || "Failed to add item", "error");
        }
    });
});

async function loadRestaurant(id) {
    try {
        const data = await apiGet("/restaurants/" + id);
        if (data && data.success && data.data) {
            document.getElementById("restaurant-name").textContent = data.data.name;
            const cuisineEl = document.getElementById("restaurant-cuisine");
            if (cuisineEl && data.data.category) {
                cuisineEl.textContent = data.data.category;
            }
        }
    } catch (e) {
        document.getElementById("restaurant-name").textContent = "Restaurant";
    }
}

async function loadMenu(id, category) {
    const grid = document.getElementById("menu-grid");
    const loading = document.getElementById("menu-loading");
    try {
        const data = await apiGet("/restaurants/" + id + "/menu", { category });
        loading.style.display = "none";
	if (data && data.data && Array.isArray(data.data.items)) {
            const items = data.data.items || [];
            if (items.length === 0) {
                grid.innerHTML = '<div class="empty-state"><div class="icon">🚫</div><p>No items in this category.</p></div>';
                return;
            }
            grid.innerHTML = items.map(f => `
                <article class="food-card">
                    <div class="card-image">🍽️</div>
                    <div class="card-title">${escape(f.name)}</div>
                    <div class="card-sub">${escape(f.category)}</div>
                    <div class="card-footer">
                        <span class="price">${formatMoney(f.price)}</span>
                        <button class="btn btn-sm btn-primary add-btn" data-id="${escape(f.itemId)}" data-name="${escape(f.name)}" data-price="${f.price}" data-category="${escape(f.category)}" data-restaurant="${escape(data.data.restaurantId)}">Add</button>
                    </div>
                </article>
            `).join("");
            
        }
else {
    grid.innerHTML = '<div class="empty-state"><p>Menu response is invalid.</p></div>';
}
    } catch (e) {
        loading.style.display = "none";
        grid.innerHTML = '<div class="empty-state"><div class="icon">⚠️</div><p>Unable to load menu.</p></div>';
        toast(e.message || "Failed to load menu", "error");
    }
}

function escape(s) { return String(s == null ? "" : s).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
