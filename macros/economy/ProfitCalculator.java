package com.donut.client.macros.economy;

import com.donut.client.macros.Macro;
import net.minecraft.client.MinecraftClient;

/**
 * ProfitCalculator - Calculates bazaar flip profits
 * Features: Tax calculation, margin analysis, ROI calculation
 */
public class ProfitCalculator extends Macro {

    private final MinecraftClient mc;

    // Hypixel Skyblock bazaar tax rates
    private static final double TAX_RATE = 0.0125; // 1.25% per transaction
    private static final double TOTAL_TAX = TAX_RATE * 2; // 2.5% total (buy + sell)

    // Settings
    private boolean includeTax = true;
    private boolean showDetailed = false;

    public ProfitCalculator() {
        super("Profit Calculator", "Calculate bazaar flip profits");
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void start() {
        System.out.println("[Profit Calculator] Started");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("[Profit Calculator] Ready");
        System.out.println("[Profit Calculator] Tax: " + (includeTax ? "Enabled (2.5%)" : "Disabled"));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("[Profit Calculator] Stopped");
    }

    @Override
    public void onTick() {
        // No continuous operations needed
    }

    /**
     * Calculate instant flip profit
     * (Buy at sell price, sell at buy price)
     */
    public Calculation calculateInstant(String productId, double buyPrice,
                                        double sellPrice, int quantity) {
        return calculate(productId, buyPrice, sellPrice, quantity, FlipType.INSTANT);
    }

    /**
     * Calculate order flip profit
     * (Place buy order, wait, place sell order)
     */
    public Calculation calculateOrder(String productId, double buyPrice,
                                      double sellPrice, int quantity) {
        return calculate(productId, buyPrice, sellPrice, quantity, FlipType.ORDER);
    }

    /**
     * Calculate flip profit with details
     */
    private Calculation calculate(String productId, double buyPrice, double sellPrice,
                                  int quantity, FlipType type) {
        // Buy costs
        double buyCost = buyPrice * quantity;
        double buyTax = includeTax ? buyCost * TAX_RATE : 0;
        double totalBuy = buyCost + buyTax;

        // Sell revenue
        double sellRevenue = sellPrice * quantity;
        double sellTax = includeTax ? sellRevenue * TAX_RATE : 0;
        double totalSell = sellRevenue - sellTax;

        // Profit calculations
        double grossProfit = sellRevenue - buyCost;
        double netProfit = totalSell - totalBuy;
        double margin = (netProfit / totalBuy) * 100; // Percentage
        double roi = (netProfit / totalBuy) * 100; // Return on investment

        // Per-item profit
        double profitPerItem = netProfit / quantity;

        Calculation calc = new Calculation(
                productId,
                type,
                quantity,
                buyPrice,
                sellPrice,
                buyCost,
                buyTax,
                totalBuy,
                sellRevenue,
                sellTax,
                totalSell,
                grossProfit,
                netProfit,
                margin,
                roi,
                profitPerItem
        );

        return calc;
    }

    /**
     * Calculate break-even sell price
     */
    public double calculateBreakEven(double buyPrice, int quantity) {
        if (!includeTax) {
            return buyPrice;
        }

        // Need to account for both taxes:
        // Cost = buyPrice * (1 + TAX_RATE)
        // Revenue = sellPrice * (1 - TAX_RATE)
        // Break-even: Revenue = Cost
        // sellPrice * (1 - TAX_RATE) = buyPrice * (1 + TAX_RATE)
        // sellPrice = buyPrice * (1 + TAX_RATE) / (1 - TAX_RATE)

        return buyPrice * (1 + TAX_RATE) / (1 - TAX_RATE);
    }

    /**
     * Calculate required sell price for target profit
     */
    public double calculateTargetSellPrice(double buyPrice, double targetProfit, int quantity) {
        // Target net profit per item
        double targetPerItem = targetProfit / quantity;

        if (!includeTax) {
            return buyPrice + targetPerItem;
        }

        // Account for taxes:
        // netProfit = sellPrice * (1 - TAX_RATE) - buyPrice * (1 + TAX_RATE)
        // Solve for sellPrice:
        // sellPrice = (netProfit + buyPrice * (1 + TAX_RATE)) / (1 - TAX_RATE)

        double totalBuy = buyPrice * (1 + TAX_RATE);
        double requiredRevenue = targetPerItem + totalBuy;
        return requiredRevenue / (1 - TAX_RATE);
    }

    /**
     * Compare two flip opportunities
     */
    public Comparison compare(Calculation calc1, Calculation calc2) {
        boolean calc1Better = calc1.netProfit > calc2.netProfit;
        double profitDiff = Math.abs(calc1.netProfit - calc2.netProfit);
        double marginDiff = Math.abs(calc1.margin - calc2.margin);

        return new Comparison(calc1, calc2, calc1Better, profitDiff, marginDiff);
    }

    /**
     * Print calculation details
     */
    public void printCalculation(Calculation calc) {
        System.out.println("========================================");
        System.out.println("PROFIT CALCULATION: " + calc.productId);
        System.out.println("========================================");
        System.out.println("Type: " + calc.type);
        System.out.println("Quantity: " + calc.quantity);
        System.out.println();

        System.out.println("BUY:");
        System.out.println("  Price/item: " + formatCoins((long) calc.buyPrice));
        System.out.println("  Subtotal: " + formatCoins((long) calc.buyCost));
        if (includeTax) {
            System.out.println("  Tax (1.25%): " + formatCoins((long) calc.buyTax));
        }
        System.out.println("  TOTAL: " + formatCoins((long) calc.totalBuy));
        System.out.println();

        System.out.println("SELL:");
        System.out.println("  Price/item: " + formatCoins((long) calc.sellPrice));
        System.out.println("  Subtotal: " + formatCoins((long) calc.sellRevenue));
        if (includeTax) {
            System.out.println("  Tax (1.25%): -" + formatCoins((long) calc.sellTax));
        }
        System.out.println("  TOTAL: " + formatCoins((long) calc.totalSell));
        System.out.println();

        System.out.println("PROFIT:");
        System.out.println("  Gross: " + formatCoins((long) calc.grossProfit));
        System.out.println("  Net: " + formatCoins((long) calc.netProfit));
        System.out.println("  Per Item: " + formatCoins((long) calc.profitPerItem));
        System.out.println("  Margin: " + String.format("%.2f%%", calc.margin));
        System.out.println("  ROI: " + String.format("%.2f%%", calc.roi));
        System.out.println("========================================");
    }

    /**
     * Print quick summary
     */
    public void printQuick(Calculation calc) {
        System.out.println(String.format("%s: Buy %s → Sell %s = %s profit (%.1f%% margin)",
                calc.productId,
                formatCoins((long) calc.buyPrice),
                formatCoins((long) calc.sellPrice),
                formatCoins((long) calc.netProfit),
                calc.margin));
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
     * Get status display
     */
    public String getStatus() {
        return "READY | Tax: " + (includeTax ? "2.5%" : "Off");
    }

    // ==================== DATA CLASSES ====================

    public enum FlipType {
        INSTANT,    // Instant buy/sell
        ORDER       // Place orders
    }

    public static class Calculation {
        // Product info
        public String productId;
        public FlipType type;
        public int quantity;

        // Prices
        public double buyPrice;
        public double sellPrice;

        // Buy costs
        public double buyCost;
        public double buyTax;
        public double totalBuy;

        // Sell revenue
        public double sellRevenue;
        public double sellTax;
        public double totalSell;

        // Profit
        public double grossProfit;
        public double netProfit;
        public double margin; // Percentage
        public double roi; // Return on investment percentage
        public double profitPerItem;

        public Calculation(String productId, FlipType type, int quantity,
                           double buyPrice, double sellPrice,
                           double buyCost, double buyTax, double totalBuy,
                           double sellRevenue, double sellTax, double totalSell,
                           double grossProfit, double netProfit, double margin,
                           double roi, double profitPerItem) {
            this.productId = productId;
            this.type = type;
            this.quantity = quantity;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.buyCost = buyCost;
            this.buyTax = buyTax;
            this.totalBuy = totalBuy;
            this.sellRevenue = sellRevenue;
            this.sellTax = sellTax;
            this.totalSell = totalSell;
            this.grossProfit = grossProfit;
            this.netProfit = netProfit;
            this.margin = margin;
            this.roi = roi;
            this.profitPerItem = profitPerItem;
        }

        public boolean isProfitable() {
            return netProfit > 0;
        }
    }

    public static class Comparison {
        public Calculation calc1;
        public Calculation calc2;
        public boolean calc1Better;
        public double profitDifference;
        public double marginDifference;

        public Comparison(Calculation calc1, Calculation calc2, boolean calc1Better,
                          double profitDiff, double marginDiff) {
            this.calc1 = calc1;
            this.calc2 = calc2;
            this.calc1Better = calc1Better;
            this.profitDifference = profitDiff;
            this.marginDifference = marginDiff;
        }
    }

    // ==================== GETTERS/SETTERS ====================

    public void setIncludeTax(boolean include) {
        this.includeTax = include;
    }

    public void setShowDetailed(boolean detailed) {
        this.showDetailed = detailed;
    }

    public static double getTaxRate() {
        return TAX_RATE;
    }

    public static double getTotalTax() {
        return TOTAL_TAX;
    }
}