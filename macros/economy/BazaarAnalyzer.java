package com.donut.client.macros.economy;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;

import java.util.*;

/**
 * BazaarAnalyzer - Analyzes bazaar market data
 * Features: Price tracking, flip detection, margin analysis
 */
public class BazaarAnalyzer extends Macro {

    private final MinecraftClient mc;

    // Market data
    private Map<String, ProductData> products = new HashMap<>();

    // Settings
    private double minMargin = 0.02; // 2% minimum margin
    private double minProfit = 50000; // 50k minimum profit
    private int scanInterval = 5000; // 5 seconds between scans

    // State
    private long lastScan = 0;

    public BazaarAnalyzer() {
        super("Bazaar Analyzer", "Analyze bazaar market data");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        products.clear();
        lastScan = 0;
        System.out.println("[Bazaar Analyzer] Started");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Bazaar Analyzer] Analyzing bazaar...");
        System.out.println("[Bazaar Analyzer] Min margin: " + (minMargin * 100) + "%");
        System.out.println("[Bazaar Analyzer] Min profit: " + formatCoins((long) minProfit));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Bazaar Analyzer] Stopped");
        printTopFlips();
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();

        // Scan periodically
        if (now - lastScan >= scanInterval) {
            scanBazaar();
            lastScan = now;
        }
    }

    /**
     * Scan bazaar for profitable items
     */
    private void scanBazaar() {
        // Simulated bazaar data - in real implementation, fetch from Hypixel API
        // For now, use example data

        updateProduct("ENCHANTED_DIAMOND", 500000, 520000, 1500, 1200);
        updateProduct("ENCHANTED_GOLD", 120000, 125000, 5000, 4800);
        updateProduct("ENCHANTED_IRON", 80000, 83000, 8000, 7500);
        updateProduct("ENCHANTED_COAL", 15000, 15500, 20000, 19000);
        updateProduct("ENCHANTED_EMERALD", 300000, 310000, 2000, 1800);
        updateProduct("ENCHANTED_REDSTONE", 25000, 26000, 10000, 9500);
        updateProduct("ENCHANTED_LAPIS", 18000, 18700, 15000, 14500);
        updateProduct("ENCHANTED_QUARTZ", 45000, 46500, 6000, 5800);

        // Mining items
        updateProduct("ENCHANTED_MITHRIL", 150000, 155000, 3000, 2900);
        updateProduct("ENCHANTED_TITANIUM", 2000000, 2050000, 500, 480);
        updateProduct("GLACITE_JEWEL", 80000, 82000, 4000, 3900);
        updateProduct("ENCHANTED_HARD_STONE", 12000, 12400, 25000, 24000);

        // Farming items
        updateProduct("ENCHANTED_WHEAT", 8000, 8300, 30000, 29000);
        updateProduct("ENCHANTED_CARROT", 9000, 9400, 28000, 27000);
        updateProduct("ENCHANTED_POTATO", 8500, 8800, 29000, 28000);
        updateProduct("ENCHANTED_SUGAR_CANE", 12000, 12500, 20000, 19500);
        updateProduct("ENCHANTED_CACTUS", 15000, 15600, 15000, 14500);

        // Combat items
        updateProduct("ENCHANTED_ROTTEN_FLESH", 7000, 7300, 35000, 34000);
        updateProduct("ENCHANTED_BONE", 11000, 11400, 22000, 21500);
        updateProduct("ENCHANTED_STRING", 9000, 9350, 25000, 24500);
        updateProduct("ENCHANTED_ENDER_PEARL", 120000, 124000, 3000, 2900);
        updateProduct("ENCHANTED_BLAZE_ROD", 180000, 186000, 2000, 1900);
    }

    /**
     * Update product data
     */
    private void updateProduct(String id, double buyPrice, double sellPrice,
                               long buyVolume, long sellVolume) {
        ProductData data = products.get(id);

        if (data == null) {
            data = new ProductData(id);
            products.put(id, data);
        }

        data.updatePrices(buyPrice, sellPrice, buyVolume, sellVolume);
    }

    /**
     * Get profitable flips
     */
    public List<FlipOpportunity> getProfitableFlips() {
        List<FlipOpportunity> flips = new ArrayList<>();

        for (ProductData data : products.values()) {
            if (data.buyPrice <= 0 || data.sellPrice <= 0) continue;

            // Calculate profit
            double profit = data.sellPrice - data.buyPrice;
            double margin = profit / data.buyPrice;

            // Check thresholds
            if (profit >= minProfit && margin >= minMargin) {
                FlipOpportunity flip = new FlipOpportunity(
                        data.id,
                        data.buyPrice,
                        data.sellPrice,
                        profit,
                        margin,
                        data.buyVolume,
                        data.sellVolume
                );

                flips.add(flip);
            }
        }

        // Sort by profit
        flips.sort((a, b) -> Double.compare(b.profit, a.profit));

        return flips;
    }

    /**
     * Get product data
     */
    public ProductData getProduct(String id) {
        return products.get(id);
    }

    /**
     * Print top flips
     */
    public void printTopFlips() {
        List<FlipOpportunity> flips = getProfitableFlips();

        System.out.println("========================================");
        System.out.println("TOP BAZAAR FLIPS");
        System.out.println("========================================");

        if (flips.isEmpty()) {
            System.out.println("No profitable flips found");
        } else {
            for (int i = 0; i < Math.min(10, flips.size()); i++) {
                FlipOpportunity flip = flips.get(i);

                System.out.println(String.format("%d. %s", i + 1, flip.productId));
                System.out.println(String.format("   Buy: %s → Sell: %s",
                        formatCoins((long) flip.buyPrice),
                        formatCoins((long) flip.sellPrice)));
                System.out.println(String.format("   Profit: %s (%.1f%% margin)",
                        formatCoins((long) flip.profit),
                        flip.margin * 100));
                System.out.println(String.format("   Volume: Buy %s | Sell %s",
                        formatVolume(flip.buyVolume),
                        formatVolume(flip.sellVolume)));
                System.out.println();
            }
        }

        System.out.println("========================================");
    }

    /**
     * Format coins
     */
    private String formatCoins(long coins) {
        if (coins >= 1000000000) {
            return String.format("%.1fB", coins / 1000000000.0);
        } else if (coins >= 1000000) {
            return String.format("%.1fM", coins / 1000000.0);
        } else if (coins >= 1000) {
            return String.format("%.1fK", coins / 1000.0);
        }
        return String.valueOf(coins);
    }

    /**
     * Format volume
     */
    private String formatVolume(long volume) {
        if (volume >= 1000000) {
            return String.format("%.1fM", volume / 1000000.0);
        } else if (volume >= 1000) {
            return String.format("%.1fK", volume / 1000.0);
        }
        return String.valueOf(volume);
    }

    /**
     * Get status display
     */
    public String getStatus() {
        int flips = getProfitableFlips().size();
        return String.format("ANALYZING | Products: %d | Flips: %d",
                products.size(), flips);
    }

    // ==================== DATA CLASSES ====================

    public static class ProductData {
        public String id;
        public double buyPrice = 0;
        public double sellPrice = 0;
        public long buyVolume = 0;
        public long sellVolume = 0;
        public long lastUpdate = 0;

        public ProductData(String id) {
            this.id = id;
        }

        public void updatePrices(double buyPrice, double sellPrice,
                                 long buyVolume, long sellVolume) {
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.buyVolume = buyVolume;
            this.sellVolume = sellVolume;
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    public static class FlipOpportunity {
        public String productId;
        public double buyPrice;
        public double sellPrice;
        public double profit;
        public double margin;
        public long buyVolume;
        public long sellVolume;

        public FlipOpportunity(String productId, double buyPrice, double sellPrice,
                               double profit, double margin, long buyVolume, long sellVolume) {
            this.productId = productId;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.profit = profit;
            this.margin = margin;
            this.buyVolume = buyVolume;
            this.sellVolume = sellVolume;
        }
    }

    // ==================== GETTERS/SETTERS ====================

    public void setMinMargin(double margin) {
        this.minMargin = margin;
    }

    public void setMinProfit(double profit) {
        this.minProfit = profit;
    }

    public void setScanInterval(int interval) {
        this.scanInterval = interval;
    }
}