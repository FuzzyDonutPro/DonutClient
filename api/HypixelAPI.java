package com.donut.client.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HypixelAPI - Complete with public fields for OrderManager
 */
public class HypixelAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger("HypixelAPI");
    private static final String BASE_URL = "https://api.hypixel.net";

    private final HttpClient httpClient;
    private String apiKey;

    private static HypixelAPI instance;

    private HypixelAPI() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static HypixelAPI getInstance() {
        if (instance == null) {
            instance = new HypixelAPI();
        }
        return instance;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * Get bazaar product - what OrderManager expects
     */
    public BazaarProduct getBazaarProduct(String productId) {
        try {
            JsonObject bazaarData = getBazaar().get();

            if (bazaarData != null && bazaarData.has("products")) {
                JsonObject products = bazaarData.getAsJsonObject("products");

                if (products.has(productId)) {
                    JsonObject productData = products.getAsJsonObject(productId);
                    return new BazaarProduct(productId, productData);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get bazaar product: " + productId, e);
        }

        return null;
    }

    public CompletableFuture<JsonObject> getBazaar() {
        String url = BASE_URL + "/skyblock/bazaar";
        return makeRequest(url);
    }

    public CompletableFuture<JsonObject> getPlayer(String uuid) {
        if (!hasApiKey()) {
            return CompletableFuture.failedFuture(new IllegalStateException("API key not set"));
        }
        String url = BASE_URL + "/player?uuid=" + uuid;
        return makeRequest(url);
    }

    private CompletableFuture<JsonObject> makeRequest(String url) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET();

            if (hasApiKey()) {
                requestBuilder.header("API-Key", apiKey);
            }

            HttpRequest request = requestBuilder.build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                return JsonParser.parseString(response.body()).getAsJsonObject();
                            } catch (Exception e) {
                                return null;
                            }
                        }
                        return null;
                    });

        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * BazaarProduct with PUBLIC FIELDS (what OrderManager expects)
     */
    public static class BazaarProduct {
        public final String productId;

        // PUBLIC FIELDS - OrderManager accesses these directly
        public double buyPrice;
        public double sellPrice;
        public double instaBuyPrice;
        public double instaSellPrice;
        public long buyVolume;
        public long sellVolume;

        private final JsonObject data;

        public BazaarProduct(String productId, JsonObject data) {
            this.productId = productId;
            this.data = data;

            // Parse prices and populate public fields
            parsePrices();
        }

        private void parsePrices() {
            try {
                if (data.has("quick_status")) {
                    JsonObject quickStatus = data.getAsJsonObject("quick_status");

                    if (quickStatus.has("buyPrice")) {
                        this.buyPrice = quickStatus.get("buyPrice").getAsDouble();
                    }
                    if (quickStatus.has("sellPrice")) {
                        this.sellPrice = quickStatus.get("sellPrice").getAsDouble();
                    }
                    if (quickStatus.has("buyVolume")) {
                        this.buyVolume = quickStatus.get("buyVolume").getAsLong();
                    }
                    if (quickStatus.has("sellVolume")) {
                        this.sellVolume = quickStatus.get("sellVolume").getAsLong();
                    }
                }

                // Insta prices from order books
                if (data.has("buy_summary") && data.getAsJsonArray("buy_summary").size() > 0) {
                    JsonObject topBuy = data.getAsJsonArray("buy_summary").get(0).getAsJsonObject();
                    this.instaBuyPrice = topBuy.get("pricePerUnit").getAsDouble();
                }

                if (data.has("sell_summary") && data.getAsJsonArray("sell_summary").size() > 0) {
                    JsonObject topSell = data.getAsJsonArray("sell_summary").get(0).getAsJsonObject();
                    this.instaSellPrice = topSell.get("pricePerUnit").getAsDouble();
                }

            } catch (Exception e) {
                LOGGER.error("Failed to parse prices for " + productId, e);
            }
        }

        public JsonObject getRawData() {
            return data;
        }
    }
}