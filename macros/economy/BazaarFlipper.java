package com.donut.client.macros.economy;

import com.donut.client.macros.Macro;
import com.donut.client.api.HypixelAPI;
import net.minecraft.client.MinecraftClient;
import java.util.*;

public class BazaarFlipper extends Macro {

    private final MinecraftClient mc;
    private final HypixelAPI api;
    private final BazaarAnalyzer analyzer;
    private FlipperState state = FlipperState.ANALYZING;
    private Queue<FlipOrder> activeOrders = new LinkedList<>();
    private double minProfit = 100000;
    private long totalProfit = 0;
    private int successfulFlips = 0;

    public enum FlipperState {
        ANALYZING, BUYING, WAITING, SELLING, IDLE
    }

    public BazaarFlipper() {
        super("Bazaar Flipper", "Automated bazaar flipping");
        this.mc = MinecraftClient.getInstance();
        this.api = HypixelAPI.getInstance();
        this.analyzer = new BazaarAnalyzer();
    }

    @Override
    public void start() {
        state = FlipperState.ANALYZING;
        System.out.println("[Flipper] Initialized");
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        analyzer.onTick();

        switch (state) {
            case ANALYZING: analyzeMarket(); break;
            case BUYING: executeBuyOrders(); break;
            case WAITING: checkOrderStatus(); break;
            case SELLING: executeSellOrders(); break;
        }
    }

    private void analyzeMarket() {
        List<BazaarAnalyzer.FlipOpportunity> recommendations = analyzer.getProfitableFlips();

        if (recommendations.size() > 10) {
            recommendations = recommendations.subList(0, 10);
        }

        List<BazaarAnalyzer.FlipOpportunity> validFlips = new ArrayList<>();

        for (BazaarAnalyzer.FlipOpportunity rec : recommendations) {
            if (rec.profit < minProfit) continue;
            validFlips.add(rec);
        }

        if (validFlips.isEmpty()) {
            state = FlipperState.IDLE;
            return;
        }

        for (int i = 0; i < Math.min(validFlips.size(), 5); i++) {
            BazaarAnalyzer.FlipOpportunity rec = validFlips.get(i);
            FlipOrder order = new FlipOrder(rec.productId, rec.buyPrice, rec.sellPrice, 1);
            activeOrders.add(order);
        }

        state = FlipperState.BUYING;
    }

    private void executeBuyOrders() {
        if (activeOrders.isEmpty()) {
            state = FlipperState.WAITING;
            return;
        }

        FlipOrder order = activeOrders.peek();
        if (!order.buyFilled) {
            order.buyFilled = true;
            activeOrders.poll();
        }
    }

    private void checkOrderStatus() {
        if (activeOrders.isEmpty()) {
            state = FlipperState.ANALYZING;
        }
    }

    private void executeSellOrders() {
        if (activeOrders.isEmpty()) {
            state = FlipperState.ANALYZING;
        }
    }

    public static class FlipOrder {
        public String productId;
        public double buyPrice;
        public double sellPrice;
        public int quantity;
        public boolean buyFilled = false;

        public FlipOrder(String productId, double buyPrice, double sellPrice, int quantity) {
            this.productId = productId;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.quantity = quantity;
        }
    }
}