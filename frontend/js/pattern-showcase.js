/* SmartFoodDeliverySystem — Pattern Showcase */
import { apiGet, apiPost, ensureSession, loadCurrentUser, currentUser, toast } from "./js/app.js";

const patterns = [
  {
    id: "abstract-factory",
    name: "Abstract Factory",
    category: "Creational",
    icon: "🏭",
    description: "Creates families of related objects (restaurant menus) without specifying concrete classes.",
    details: "PizzaHubFactory and SpiceHubFactory implement RestaurantFactory, each producing restaurant-specific Food and Drink objects. The DatabaseRestaurantRepository loads menu data from SQLite while preserving factory behavior.",
    demo: async () => {
      log("Loading restaurants from database via Abstract Factory...");
      const data = await apiGet("/restaurants");
      if (data.success) {
        log("Restaurants found:");
        data.data.forEach(r => log(`  • ${r.name} (${r.category})`));
        
        log("\nLoading VEG menu for Pizza Hub...");
        const menu = await apiGet("/restaurants/pizza/menu?category=VEG");
        if (menu.success) {
          log(`Category: ${menu.data.category}`);
          menu.data.items.forEach(item => {
            log(`  • ${item.name} - Rs.${item.price.toFixed(2)} [${item.category}]`);
          });
        }
        log("\n✅ Abstract Factory: Restaurant factories provide menus from database");
      }
    }
  },
  {
    id: "factory-method",
    name: "Factory Method",
    category: "Creational",
    icon: "🔧",
    description: "Defines interface for creating orders, lets subclasses decide which class to instantiate.",
    details: "VegOrderCreator, NonVegOrderCreator, ComboOrderCreator implement OrderCreator. FoodDeliveryService selects the creator based on item category when creating orders.",
    demo: async () => {
      log("Factory Method: Order creation per category");
      log("• VegOrderCreator → creates VegOrder");
      log("• NonVegOrderCreator → creates NonVegOrder");
      log("• ComboOrderCreator → creates ComboOrder");
      log("\nThis is triggered when you place an order at checkout.");
      log("The service selects the correct creator based on each item's category.");
      log("✅ Factory Method: Category-specific order creation");
    }
  },
  {
    id: "builder",
    name: "Builder",
    category: "Creational",
    icon: "🏗️",
    description: "Constructs complex orders step-by-step with validation.",
    details: "OrderBuilder validates all required fields (orderId, customerName, foodItem, quantity, orderType) before constructing the Order. Used in createOrders() for each cart item.",
    demo: async () => {
      log("Builder Pattern: Order construction with validation");
      log("Step 1: Set orderId");
      log("Step 2: Set customerName");
      log("Step 3: Set foodItem (canonical from backend)");
      log("Step 4: Set quantity");
      log("Step 5: Set orderType (VEG/NON_VEG/COMBO)");
      log("Step 6: .build() → validates all fields, throws if invalid");
      log("✅ Builder: Ensures complete, valid orders before creation");
    }
  },
  {
    id: "singleton",
    name: "Singleton",
    category: "Creational",
    icon: "🔒",
    description: "Ensures single OrderManager instance for centralized order registration.",
    details: "OrderManager.getInstance() returns the same instance everywhere. Used in createOrders() to register each order centrally.",
    demo: async () => {
      log("Singleton Pattern: OrderManager");
      log("OrderManager.getInstance() always returns same instance");
      log("Used to register all orders in one place");
      log("✅ Singleton: Single registry for all orders");
    }
  },
  {
    id: "chain-of-responsibility",
    name: "Chain of Responsibility",
    category: "Behavioral",
    icon: "⛓️",
    description: "Passes order through validation pipeline: Payment → Restaurant → Delivery.",
    details: "PaymentHandler → RestaurantHandler → DeliveryHandler. Each validates a concern. If any fails, chain stops. Used in validate() before order creation.",
    demo: async () => {
      log("Chain of Responsibility: Order Validation Pipeline");
      log("Step 1: PaymentHandler → validates payment info");
      log("Step 2: RestaurantHandler → validates restaurant exists");
      log("Step 3: DeliveryHandler → validates delivery possible");
      log("\nIf any handler fails, chain stops and order is rejected.");
      log("✅ Chain of Responsibility: Decoupled validation steps");
    }
  },
  {
    id: "proxy",
    name: "Proxy",
    category: "Structural",
    icon: "🛡️",
    description: "Controls access to payment gateway, adds validation/logging.",
    details: "PaymentGatewayProxy wraps PaymentGatewayImpl. Adds amount validation, logging, and prevents direct gateway access. COD bypasses proxy.",
    demo: async () => {
      log("Proxy Pattern: Payment Gateway");
      log("PaymentGatewayProxy wraps PaymentGatewayImpl");
      log("• Validates amount > 0 and ≤ limit");
      log("• Logs all payment attempts");
      log("• Prevents direct gateway access");
      log("• COD bypasses proxy entirely");
      log("✅ Proxy: Controlled, validated payment access");
    }
  },
  {
    id: "strategy",
    name: "Strategy",
    category: "Behavioral",
    icon: "🎯",
    description: "Interchangeable payment algorithms: UPI, Card, Cash on Delivery.",
    details: "PaymentService context uses UPIPaymentStrategy, CardPaymentStrategy, or CashOnDeliveryStrategy. Selected at checkout based on user choice.",
    demo: async () => {
      log("Strategy Pattern: Payment Processing");
      log("PaymentService sets strategy at runtime:");
      log("• UPI: Validates UPI ID, simulates payment");
      log("• Card: Validates last 4 digits, simulates payment");
      log("• COD: No proxy, marks pay-on-delivery");
      log("\nStrategy selected by user at checkout.");
      log("✅ Strategy: Swappable payment algorithms");
    }
  },
  {
    id: "observer",
    name: "Observer",
    category: "Behavioral",
    icon: "👁️",
    description: "Notifies customer and restaurant when order status changes.",
    details: "OrderStatusSubject extends Observable. CustomerNotificationObserver and RestaurantNotificationObserver register for updates. advanceStatus() triggers notifications.",
    demo: async () => {
      log("Observer Pattern: Order Status Notifications");
      log("Subject: OrderStatusSubject (extends Observable)");
      log("Observers:");
      log("  • CustomerNotificationObserver → notifies customer");
      log("  • RestaurantNotificationObserver → notifies restaurant");
      log("\nWhen advanceStatus() is called, both observers are notified.");
      log("✅ Observer: Decoupled status notifications");
    }
  },
  {
    id: "prototype",
    name: "Prototype",
    category: "Creational",
    icon: "📋",
    description: "Clones existing orders for reordering without rebuilding.",
    details: "Order implements Prototype<Order> with copy() method. Reorder feature clones the original order for review before placing.",
    demo: async () => {
      log("Prototype Pattern: Order Reordering");
      log("Order implements Prototype<Order> with copy()");
      log("Creates independent clone with same data");
      log("Allows review/modify before placing new order");
      log("✅ Prototype: Clone orders for reordering");
    }
  },
  {
    id: "memento",
    name: "Memento",
    category: "Behavioral",
    icon: "💾",
    description: "Captures and restores cart state snapshots (Save/Restore Cart).",
    details: "ShoppingCart (Originator) creates CartMemento (Memento). DatabaseCartRepository (Caretaker) persists snapshots in SQLite. Token format: cart:itemId:name:price:qty|...",
    demo: async () => {
      log("Memento Pattern: Cart Snapshots");
      log("Originator: ShoppingCart");
      log("Memento: CartMemento (immutable snapshot)");
      log("Caretaker: DatabaseCartRepository (SQLite persistence)");
      log("\nToken format: cart:itemId:name:price:qty|itemId:name:price:qty");
      log("✅ Memento: Persistent cart snapshots");
    }
  }
];

const grid = document.getElementById("patterns-grid");
const demoSection = document.getElementById("demo-output");
const demoTitle = document.getElementById("demo-title");
const demoConsole = document.getElementById("demo-console");
const closeBtn = document.getElementById("close-demo");

function log(msg) {
  demoConsole.textContent += msg + "\n";
  demoConsole.scrollTop = demoConsole.scrollHeight;
}

function clearLog() {
  demoConsole.textContent = "";
}

function showDemo(pattern) {
  grid.style.display = "none";
  demoSection.style.display = "block";
  demoTitle.textContent = `${pattern.icon} ${pattern.name} — Live Demo`;
  clearLog();
  log(`🔄 Running ${pattern.name} demo...\n`);
  
  pattern.demo().then(() => {
    log(`\n✅ ${pattern.name} demo complete.`);
  }).catch(e => {
    log(`\n❌ Error: ${e.message}`);
  });
}

function hideDemo() {
  demoSection.style.display = "none";
  grid.style.display = "grid";
  clearLog();
}

function renderPatterns() {
  grid.innerHTML = patterns.map(p => `
    <div class="pattern-card restaurant-card" data-id="${p.id}" style="cursor:pointer;transition:transform .2s,box-shadow .2s;">
      <div style="font-size:2.5rem;margin-bottom:12px;">${p.icon}</div>
      <div class="card-title">${p.name}</div>
      <div class="card-sub" style="font-size:0.8rem;text-transform:uppercase;letter-spacing:.5px;color:var(--primary);">${p.category}</div>
      <p style="margin-top:12px;color:var(--text-muted);font-size:0.9rem;">${p.description}</p>
      <div style="margin-top:16px;padding-top:12px;border-top:1px solid var(--card-border);font-size:0.8rem;color:var(--text-muted);">
        Click to run live demo →
      </div>
    </div>
  `).join("");

  grid.querySelectorAll(".pattern-card").forEach(card => {
    card.addEventListener("click", () => {
      const pattern = patterns.find(p => p.id === card.dataset.id);
      if (pattern) showDemo(pattern);
    });
    
    card.addEventListener("mouseenter", () => {
      card.style.transform = "translateY(-4px)";
      card.style.boxShadow = "var(--shadow)";
    });
    card.addEventListener("mouseleave", () => {
      card.style.transform = "none";
      card.style.boxShadow = "none";
    });
  });
}

closeBtn.addEventListener("click", hideDemo);

document.addEventListener("DOMContentLoaded", async () => {
  await loadCurrentUser();
  renderPatterns();
});