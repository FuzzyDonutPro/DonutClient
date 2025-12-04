package com.donut.client.macros.economy;

import com.donut.client.macros.Macro;
import com.donut.client.api.HypixelAPI;
import net.minecraft.client.MinecraftClient;

import java.util.*;

/**
 * OrderManager - Manages bazaar buy/sell orders
 * Features: Order tracking, auto-cancel, auto-relist, price adjustment
 */
public class OrderManager extends Macro {

    private final MinecraftClient mc;
    private final HypixelAPI api;

    // Order tracking
    private Map<String, List<Order>> activeOrders = new HashMap<>();
    private List<Order> orderHistory = new ArrayList<>();

    // Settings
    private boolean autoCancel = true;
    private boolean autoRelist = true;
    private boolean dynamicPricing = true;
    private long orderTimeout = 300000; // 5 minutes
    private double priceAdjustmentPercent = 0.01; // 1% price adjustment

    // Order limits
    private int maxOrdersPerProduct = 10;
    private int maxTotalOrders = 100;

    // Statistics
    private int totalOrdersPlaced = 0;
    private int totalOrdersFilled = 0;
    private int totalOrdersCancelled = 0;

    public OrderManager() {
        super("Order Manager", "Manage bazaar orders");
        this.mc = MinecraftClient.getInstance();
        this.api = HypixelAPI.getInstance();
    }

    @Override
    public void start() {
        activeOrders.clear();
        orderHistory.clear();
        System.out.println("[Order Manager] Initialized");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Order Manager] Starting...");
        System.out.println("[Order Manager] Auto-cancel: " + autoCancel);
        System.out.println("[Order Manager] Auto-relist: " + autoRelist);

        // Load existing orders
        loadActiveOrders();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Order Manager] Stopped");
        printStatistics();
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        // Check order status periodically
        if (System.currentTimeMillis() % 100 == 0) {
            checkOrderStatus();
        }

        // Handle timeouts
        if (autoCancel) {
            handleTimeouts();
        }

        // Adjust prices
        if (dynamicPricing) {
            adjustPrices();
        }
    }

    /**
     * Load active orders from API
     */
    private void loadActiveOrders() {
        System.out.println("[Order Manager] Loading active orders...");

        // TODO: Load from Hypixel API
        // api.getPlayerOrders()

        System.out.println("[Order Manager] Loaded " + getTotalActiveOrders() + " orders");
    }

    /**
     * Place buy order
     */
    public String placeBuyOrder(String productId, double price, int quantity) {
        // Check limits
        if (!checkOrderLimits(productId)) {
            System.out.println("[Order Manager] Order limit reached for " + productId);
            return null;
        }

        // Validate price
        HypixelAPI.BazaarProduct product = api.getBazaarProduct(productId);
        if (product != null && price > product.sellPrice) {
            System.out.println("[Order Manager] WARNING: Buy price higher than instant sell!");
        }

        // Create order
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(
                orderId,
                productId,
                OrderType.BUY,
                price,
                quantity,
                quantity, // Initially unfilled
                System.currentTimeMillis(),
                OrderStatus.ACTIVE
        );

        // Add to tracking
        activeOrders.computeIfAbsent(productId, k -> new ArrayList<>()).add(order);
        totalOrdersPlaced++;

        System.out.println("[Order Manager] Buy order placed: " + productId +
                " x" + quantity + " @ " + formatCoins((long) price));

        // TODO: Actually place order via API

        return orderId;
    }

    /**
     * Place sell order
     */
    public String placeSellOrder(String productId, double price, int quantity) {
        // Check limits
        if (!checkOrderLimits(productId)) {
            System.out.println("[Order Manager] Order limit reached for " + productId);
            return null;
        }

        // Validate price
        HypixelAPI.BazaarProduct product = api.getBazaarProduct(productId);
        if (product != null && price < product.buyPrice) {
            System.out.println("[Order Manager] WARNING: Sell price lower than instant buy!");
        }

        // Create order
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(
                orderId,
                productId,
                OrderType.SELL,
                price,
                quantity,
                quantity, // Initially unfilled
                System.currentTimeMillis(),
                OrderStatus.ACTIVE
        );

        // Add to tracking
        activeOrders.computeIfAbsent(productId, k -> new ArrayList<>()).add(order);
        totalOrdersPlaced++;

        System.out.println("[Order Manager] Sell order placed: " + productId +
                " x" + quantity + " @ " + formatCoins((long) price));

        // TODO: Actually place order via API

        return orderId;
    }

    /**
     * Cancel order
     */
    public boolean cancelOrder(String orderId) {
        // Find order
        Order order = findOrder(orderId);
        if (order == null) {
            System.out.println("[Order Manager] Order not found: " + orderId);
            return false;
        }

        if (order.status != OrderStatus.ACTIVE) {
            System.out.println("[Order Manager] Order not active: " + orderId);
            return false;
        }

        // Cancel order
        order.status = OrderStatus.CANCELLED;
        order.cancelTime = System.currentTimeMillis();

        // Remove from active
        List<Order> productOrders = activeOrders.get(order.productId);
        if (productOrders != null) {
            productOrders.remove(order);
        }

        // Add to history
        orderHistory.add(order);
        totalOrdersCancelled++;

        System.out.println("[Order Manager] Order cancelled: " + order.productId);

        // TODO: Actually cancel via API

        return true;
    }

    /**
     * Check order status
     */
    private void checkOrderStatus() {
        List<Order> toRemove = new ArrayList<>();

        for (List<Order> orders : activeOrders.values()) {
            for (Order order : orders) {
                if (order.status != OrderStatus.ACTIVE) continue;

                // TODO: Check order status via API
                // For now, simulate random fills
                if (Math.random() < 0.01) { // 1% chance per tick
                    order.quantityRemaining--;

                    if (order.quantityRemaining <= 0) {
                        // Order filled
                        order.status = OrderStatus.FILLED;
                        order.fillTime = System.currentTimeMillis();
                        toRemove.add(order);
                        totalOrdersFilled++;

                        System.out.println("[Order Manager] Order filled: " + order.productId +
                                " (" + order.type + ")");

                        // Auto-relist if enabled
                        if (autoRelist) {
                            relistOrder(order);
                        }
                    }
                }
            }
        }

        // Remove filled orders
        for (Order order : toRemove) {
            List<Order> productOrders = activeOrders.get(order.productId);
            if (productOrders != null) {
                productOrders.remove(order);
            }
            orderHistory.add(order);
        }
    }

    /**
     * Handle order timeouts
     */
    private void handleTimeouts() {
        long now = System.currentTimeMillis();
        List<String> toCancel = new ArrayList<>();

        for (List<Order> orders : activeOrders.values()) {
            for (Order order : orders) {
                if (order.status == OrderStatus.ACTIVE) {
                    long elapsed = now - order.createTime;

                    if (elapsed > orderTimeout) {
                        System.out.println("[Order Manager] Order timeout: " + order.productId);
                        toCancel.add(order.orderId);
                    }
                }
            }
        }

        // Cancel timed out orders
        for (String orderId : toCancel) {
            cancelOrder(orderId);
        }
    }

    /**
     * Adjust prices dynamically
     */
    private void adjustPrices() {
        for (Map.Entry<String, List<Order>> entry : activeOrders.entrySet()) {
            String productId = entry.getKey();
            List<Order> orders = entry.getValue();

            // Get current market prices
            HypixelAPI.BazaarProduct product = api.getBazaarProduct(productId);
            if (product == null) continue;

            for (Order order : orders) {
                if (order.status != OrderStatus.ACTIVE) continue;

                boolean shouldAdjust = false;
                double newPrice = order.price;

                if (order.type == OrderType.BUY) {
                    // If our buy price is too low, increase it
                    if (order.price < product.buyPrice * 0.95) {
                        newPrice = product.buyPrice * (1 - priceAdjustmentPercent);
                        shouldAdjust = true;
                    }
                } else {
                    // If our sell price is too high, decrease it
                    if (order.price > product.sellPrice * 1.05) {
                        newPrice = product.sellPrice * (1 + priceAdjustmentPercent);
                        shouldAdjust = true;
                    }
                }

                if (shouldAdjust) {
                    System.out.println("[Order Manager] Adjusting price: " + productId +
                            " " + formatCoins((long) order.price) + " → " + formatCoins((long) newPrice));

                    // Cancel and re-place at new price
                    cancelOrder(order.orderId);

                    if (order.type == OrderType.BUY) {
                        placeBuyOrder(productId, newPrice, order.quantityRemaining);
                    } else {
                        placeSellOrder(productId, newPrice, order.quantityRemaining);
                    }
                }
            }
        }
    }

    /**
     * Relist order with adjusted price
     */
    private void relistOrder(Order order) {
        // Get current market price
        HypixelAPI.BazaarProduct product = api.getBazaarProduct(order.productId);
        if (product == null) return;

        double newPrice = order.price;

        if (order.type == OrderType.BUY) {
            // Relist buy at slightly higher price
            newPrice = order.price * 1.01;
        } else {
            // Relist sell at slightly lower price
            newPrice = order.price * 0.99;
        }

        System.out.println("[Order Manager] Auto-relisting: " + order.productId);

        if (order.type == OrderType.BUY) {
            placeBuyOrder(order.productId, newPrice, order.quantity);
        } else {
            placeSellOrder(order.productId, newPrice, order.quantity);
        }
    }

    /**
     * Check if order limits allow new order
     */
    private boolean checkOrderLimits(String productId) {
        // Check per-product limit
        List<Order> productOrders = activeOrders.get(productId);
        if (productOrders != null && productOrders.size() >= maxOrdersPerProduct) {
            return false;
        }

        // Check total limit
        return getTotalActiveOrders() < maxTotalOrders;
    }

    /**
     * Find order by ID
     */
    private Order findOrder(String orderId) {
        for (List<Order> orders : activeOrders.values()) {
            for (Order order : orders) {
                if (order.orderId.equals(orderId)) {
                    return order;
                }
            }
        }
        return null;
    }

    /**
     * Get total active orders
     */
    public int getTotalActiveOrders() {
        int total = 0;
        for (List<Order> orders : activeOrders.values()) {
            total += orders.size();
        }
        return total;
    }

    /**
     * Get orders for product
     */
    public List<Order> getOrders(String productId) {
        return activeOrders.getOrDefault(productId, new ArrayList<>());
    }

    /**
     * Get all active orders
     */
    public List<Order> getAllActiveOrders() {
        List<Order> all = new ArrayList<>();
        for (List<Order> orders : activeOrders.values()) {
            all.addAll(orders);
        }
        return all;
    }

    /**
     * Cancel all orders for product
     */
    public void cancelAllOrders(String productId) {
        List<Order> orders = activeOrders.get(productId);
        if (orders == null) return;

        List<String> orderIds = new ArrayList<>();
        for (Order order : orders) {
            orderIds.add(order.orderId);
        }

        for (String orderId : orderIds) {
            cancelOrder(orderId);
        }
    }

    /**
     * Cancel all orders
     */
    public void cancelAllOrders() {
        List<String> orderIds = new ArrayList<>();

        for (Order order : getAllActiveOrders()) {
            orderIds.add(order.orderId);
        }

        for (String orderId : orderIds) {
            cancelOrder(orderId);
        }
    }

    /**
     * Format coins
     */
    private String formatCoins(long coins) {
        if (coins >= 1000000) {
            return String.format("%.1fM", coins / 1000000.0);
        } else if (coins >= 1000) {
            return String.format("%.1fK", coins / 1000.0);
        }
        return String.valueOf(coins);
    }

    /**
     * Print statistics
     */
    private void printStatistics() {
        System.out.println("========================================");
        System.out.println("ORDER MANAGER STATISTICS");
        System.out.println("========================================");
        System.out.println("Total Orders Placed: " + totalOrdersPlaced);
        System.out.println("Orders Filled: " + totalOrdersFilled);
        System.out.println("Orders Cancelled: " + totalOrdersCancelled);
        System.out.println("Active Orders: " + getTotalActiveOrders());

        if (totalOrdersPlaced > 0) {
            double fillRate = (double) totalOrdersFilled / totalOrdersPlaced * 100;
            System.out.println("Fill Rate: " + String.format("%.1f%%", fillRate));
        }

        System.out.println("\nOrders by Product:");
        for (Map.Entry<String, List<Order>> entry : activeOrders.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue().size());
        }

        System.out.println("========================================");
    }

    /**
     * Get status display
     */
    public String getStatus() {
        return String.format("MANAGING | Active: %d | Filled: %d | Cancelled: %d",
                getTotalActiveOrders(), totalOrdersFilled, totalOrdersCancelled);
    }

    // ==================== DATA CLASSES ====================

    public enum OrderType {
        BUY,
        SELL
    }

    public enum OrderStatus {
        ACTIVE,
        FILLED,
        CANCELLED,
        EXPIRED
    }

    public static class Order {
        public String orderId;
        public String productId;
        public OrderType type;
        public double price;
        public int quantity;
        public int quantityRemaining;
        public long createTime;
        public long fillTime = 0;
        public long cancelTime = 0;
        public OrderStatus status;

        public Order(String orderId, String productId, OrderType type, double price,
                     int quantity, int quantityRemaining, long createTime, OrderStatus status) {
            this.orderId = orderId;
            this.productId = productId;
            this.type = type;
            this.price = price;
            this.quantity = quantity;
            this.quantityRemaining = quantityRemaining;
            this.createTime = createTime;
            this.status = status;
        }

        public boolean isPartiallyFilled() {
            return quantityRemaining < quantity && quantityRemaining > 0;
        }

        public int getFilledQuantity() {
            return quantity - quantityRemaining;
        }
    }

    // ==================== GETTERS/SETTERS ====================

    public void setAutoCancel(boolean auto) {
        this.autoCancel = auto;
    }

    public void setAutoRelist(boolean auto) {
        this.autoRelist = auto;
    }

    public void setDynamicPricing(boolean dynamic) {
        this.dynamicPricing = dynamic;
    }

    public void setOrderTimeout(long timeout) {
        this.orderTimeout = timeout;
    }

    public void setPriceAdjustmentPercent(double percent) {
        this.priceAdjustmentPercent = percent;
    }

    public void setMaxOrdersPerProduct(int max) {
        this.maxOrdersPerProduct = max;
    }

    public void setMaxTotalOrders(int max) {
        this.maxTotalOrders = max;
    }
}