package memento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CartMemento {

    private final List<CartItem> savedItems;

    CartMemento(List<CartItem> items) {
        List<CartItem> copies = new ArrayList<>();

        for (CartItem item : items) {
            copies.add(item.copy());
        }

        this.savedItems = Collections.unmodifiableList(copies);
    }

    public List<CartItem> getSavedItems() {
        List<CartItem> copies = new ArrayList<>();

        for (CartItem item : savedItems) {
            copies.add(item.copy());
        }

        return Collections.unmodifiableList(copies);
    }
}