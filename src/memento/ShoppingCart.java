package memento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShoppingCart {

    private final List<CartItem> items = new ArrayList<>();

    public void addItem(CartItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Cart item cannot be null.");
        }

        items.add(item.copy());
    }

    public void removeItem(String itemId) {
        items.removeIf(item -> item.getItemId().equals(itemId));
    }

    public void updateQuantity(String itemId, int quantity) {
        for (CartItem item : items) {
            if (item.getItemId().equals(itemId)) {
                item.setQuantity(quantity);
                return;
            }
        }

        throw new IllegalArgumentException("Item not found in cart: " + itemId);
    }

    public CartMemento save() {
        return new CartMemento(items);
    }

    public void restore(CartMemento memento) {
        if (memento == null) {
            throw new IllegalArgumentException("Cart snapshot cannot be null.");
        }

        items.clear();
        items.addAll(memento.getSavedItems());
    }

    public List<CartItem> getItems() {
        List<CartItem> copies = new ArrayList<>();

        for (CartItem item : items) {
            copies.add(item.copy());
        }

        return Collections.unmodifiableList(copies);
    }

    public double getTotal() {
        double total = 0;

        for (CartItem item : items) {
            total += item.getSubtotal();
        }

        return total;
    }

    public void displayCart() {
        if (items.isEmpty()) {
            System.out.println("Shopping cart is empty.");
            return;
        }

        System.out.println("----- Shopping Cart -----");

        for (CartItem item : items) {
            System.out.println(item);
        }

        System.out.println("Total: Rs." + getTotal());
    }
}