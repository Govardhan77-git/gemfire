package com.gemfire.demo.config;

import com.gemfire.demo.model.Product;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.FunctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GemFire Feature: Server-Side Function Execution.
 *
 * Functions execute business logic WHERE THE DATA LIVES — on GemFire servers —
 * instead of pulling data to the client. Reduces network traffic significantly.
 *
 * Arguments[0] = category (String)
 * Arguments[1] = adjustment percentage (Double, e.g. 10.0 = +10%)
 */
@Component
public class PriceAdjustmentFunction implements Function<Object> {

    private static final Logger log = LoggerFactory.getLogger(PriceAdjustmentFunction.class);
    public static final String FUNCTION_ID = "PriceAdjustmentFunction";

    private final Cache cache;

    public PriceAdjustmentFunction(Cache cache) {
        this.cache = cache;
    }

    @PostConstruct
    public void register() {
        FunctionService.registerFunction(this);
        log.info("GemFire Function registered: {}", FUNCTION_ID);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(FunctionContext<Object> context) {
        Object[] args = (Object[]) context.getArguments();
        String category = (String) args[0];
        double adjustmentPct = ((Number) args[1]).doubleValue();

        Region<String, Product> productsRegion = cache.getRegion("Products");
        List<String> updatedIds = new ArrayList<>();

        if (productsRegion != null) {
            for (Map.Entry<String, Product> entry : productsRegion.entrySet()) {
                Product product = entry.getValue();
                if (product != null && category.equalsIgnoreCase(product.getCategory())) {
                    int currentPrice = product.getPrice() != null ? product.getPrice() : 0;
                    int newPrice = (int) Math.round(currentPrice * (1 + adjustmentPct / 100));
                    product.setPrice(Math.max(0, newPrice));
                    productsRegion.put(entry.getKey(), product);
                    updatedIds.add(entry.getKey());
                }
            }
        }

        log.info("🔧 [Function] PriceAdjustment: category={}, pct={}%, updated={}",
                category, adjustmentPct, updatedIds.size());
        context.getResultSender().lastResult(updatedIds);
    }

    @Override public String getId()        { return FUNCTION_ID; }
    @Override public boolean hasResult()   { return true; }
    @Override public boolean isHA()        { return false; }
}
