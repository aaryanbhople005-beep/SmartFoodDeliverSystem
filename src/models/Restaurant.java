package models;

/**
 * Restaurant domain model.
 *
 * Previously this class was an empty placeholder. It has been enhanced with the
 * basic attributes needed by the browser interface (id, name, description and
 * category) while keeping the same package and class name so existing code is
 * unaffected.
 *
 * Restaurant instances are assembled by the repository layer from the existing
 * Abstract Factory implementations (PizzaHubFactory / SpiceHubFactory).
 */
public class Restaurant {

    private final String id;
    private final String name;
    private final String description;
    private final String category;

    public Restaurant(String id, String name, String description, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return name + " (" + category + ")";
    }
}