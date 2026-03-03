package com.gemfire.demo;

import com.gemfire.demo.model.ProductRequest;
import com.gemfire.demo.util.NullSafeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for NULL / empty value handling for int and boolean fields.
 *
 * <p>Covers:
 * <ul>
 *   <li>NullSafeUtil helper methods</li>
 *   <li>ProductRequest.resolveDefaults() business logic</li>
 * </ul>
 */
class NullHandlingTest {

    // ── NullSafeUtil Tests ────────────────────────────────────────────────────

    @Nested
    @DisplayName("NullSafeUtil - Integer handling")
    class IntegerHandling {

        @Test
        @DisplayName("null Integer returns fallback")
        void nullIntegerReturnsFallback() {
            assertThat(NullSafeUtil.defaultInt(null, 0)).isEqualTo(0);
            assertThat(NullSafeUtil.defaultInt(null, 99)).isEqualTo(99);
        }

        @Test
        @DisplayName("non-null Integer returns its value")
        void nonNullIntegerReturnsValue() {
            assertThat(NullSafeUtil.defaultInt(42, 0)).isEqualTo(42);
            assertThat(NullSafeUtil.defaultInt(0, 99)).isEqualTo(0); // explicit 0 is preserved
        }

        @Test
        @DisplayName("negative Integer is preserved (not treated as empty)")
        void negativeIntegerPreserved() {
            assertThat(NullSafeUtil.defaultInt(-1, 0)).isEqualTo(-1);
        }

        @Test
        @DisplayName("nonNegativeInt returns 0 for null")
        void nonNegativeIntHandlesNull() {
            assertThat(NullSafeUtil.nonNegativeInt(null)).isEqualTo(0);
            assertThat(NullSafeUtil.nonNegativeInt(-5)).isEqualTo(0);
            assertThat(NullSafeUtil.nonNegativeInt(10)).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("NullSafeUtil - Boolean handling")
    class BooleanHandling {

        @Test
        @DisplayName("null Boolean returns fallback")
        void nullBooleanReturnsFallback() {
            assertThat(NullSafeUtil.defaultBool(null, false)).isFalse();
            assertThat(NullSafeUtil.defaultBool(null, true)).isTrue();
        }

        @Test
        @DisplayName("explicit false is preserved (not overridden by default true)")
        void explicitFalsePreserved() {
            assertThat(NullSafeUtil.defaultBool(Boolean.FALSE, true)).isFalse();
        }

        @Test
        @DisplayName("explicit true is preserved (not overridden by default false)")
        void explicitTruePreserved() {
            assertThat(NullSafeUtil.defaultBool(Boolean.TRUE, false)).isTrue();
        }
    }

    // ── ProductRequest.resolveDefaults() Tests ────────────────────────────────

    @Nested
    @DisplayName("ProductRequest.resolveDefaults()")
    class ResolveDefaults {

        @Test
        @DisplayName("all-null payload gets sensible defaults")
        void allNullGetsDefaults() {
            ProductRequest req = new ProductRequest();
            // All fields null — simulates: { "name": "X", "category": "Y" }
            req.resolveDefaults();

            assertThat(req.getPrice()).isEqualTo(0);
            assertThat(req.getStockCount()).isEqualTo(0);
            assertThat(req.getRating()).isNull(); // intentionally left null
            assertThat(req.getActive()).isTrue(); // new products active by default
            assertThat(req.getFeatured()).isFalse();
            assertThat(req.getInStock()).isFalse(); // 0 stock → not in stock
        }

        @Test
        @DisplayName("explicit false for active is preserved after resolveDefaults")
        void explicitFalseActivePreserved() {
            ProductRequest req = new ProductRequest();
            req.setActive(Boolean.FALSE);
            req.resolveDefaults();

            assertThat(req.getActive()).isFalse();
        }

        @Test
        @DisplayName("inStock derived from stockCount when not explicitly set")
        void inStockDerivedFromStock() {
            ProductRequest req = new ProductRequest();
            req.setStockCount(50);
            req.resolveDefaults();

            assertThat(req.getInStock()).isTrue(); // 50 > 0
        }

        @Test
        @DisplayName("inStock=false preserved even when stockCount > 0 (manual override)")
        void explicitInStockFalsePreserved() {
            ProductRequest req = new ProductRequest();
            req.setStockCount(50);
            req.setInStock(Boolean.FALSE); // explicit override
            req.resolveDefaults();

            assertThat(req.getInStock()).isFalse(); // explicit false wins
        }

        @Test
        @DisplayName("explicit price=0 is preserved, not replaced by default")
        void explicitZeroPricePreserved() {
            ProductRequest req = new ProductRequest();
            req.setPrice(0); // explicitly set to 0 (free product)
            req.resolveDefaults();

            assertThat(req.getPrice()).isEqualTo(0);
        }

        @Test
        @DisplayName("validate throws on missing name")
        void validateThrowsOnMissingName() {
            ProductRequest req = new ProductRequest();
            req.setCategory("Electronics");
            assertThatThrownBy(req::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("validate throws on blank category")
        void validateThrowsOnBlankCategory() {
            ProductRequest req = new ProductRequest();
            req.setName("Widget");
            req.setCategory("  "); // blank
            assertThatThrownBy(req::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("category");
        }
    }
}
