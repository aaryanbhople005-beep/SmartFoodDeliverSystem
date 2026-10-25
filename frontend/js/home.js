/* SmartFoodDeliverySystem — home page */
import { loadCart, renderCartBadge, addToCart, formatMoney, ensureSession, toast } from "./app.js";

document.addEventListener("DOMContentLoaded", async () => {
    await loadCart();
    renderCartBadge();

    const featured = [
        { id: "P101", name: "Margherita Pizza", price: 200, category: "VEG", restaurant: "Pizza Hub" },
        { id: "P201", name: "Chicken Pizza", price: 350, category: "NON_VEG", restaurant: "Pizza Hub" },
        { id: "S101", name: "Paneer Tikka", price: 220, category: "VEG", restaurant: "Spice Hub" },
        { id: "S201", name: "Chicken Biryani", price: 280, category: "NON_VEG", restaurant: "Spice Hub" },
        { id: "P301", name: "Veg Pizza Combo", price: 399, category: "COMBO", restaurant: "Pizza Hub" },
        { id: "S301", name: "Veg Thali Combo", price: 299, category: "COMBO", restaurant: "Spice Hub" }
    ];

    const grid = document.getElementById("featured-grid");
    if (!grid) return;

    grid.innerHTML = featured.map(f => `
        <article class="food-card">
            <div class="card-image">🍕</div>
            <div class="card-title">${escape(f.name)}</div>
            <div class="card-sub">${escape(f.category)} &bull; ${escape(f.restaurant)}</div>
            <div class="card-footer">
                <span class="price">${formatMoney(f.price)}</span>
                <button class="btn btn-sm btn-primary add-btn" data-id="${f.id}" data-name="${escape(f.name)}" data-price="${f.price}" data-category="${f.category}" data-restaurant="${escape(f.restaurant)}">Add</button>
            </div>
        </article>
    `).join("");

    grid.addEventListener("click", async (e) => {
        const btn = e.target.closest(".add-btn");
        if (!btn) return;
        const item = {
            itemId: btn.dataset.id,
            name: btn.dataset.name,
            price: parseFloat(btn.dataset.price),
            quantity: 1,
            category: btn.dataset.category,
            restaurant: btn.dataset.restaurant,
            subtotal: parseFloat(btn.dataset.price)
        };
        try {
            await addToCart(item);
            toast("Added to cart", "success");
        } catch (err) {
            toast(err.message || "Failed to add item", "error");
        }
    });
});

function escape(s) { return String(s == null ? "" : s).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;"); }
