package com.gemfire.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming request DTO for Product creation/update.
 *
 * <p><strong>NULL / Empty Handling for int and boolean Datatypes:</strong>
 *
 * <p>When a client sends {@code "price": null} or omits "price" entirely in the JSON body,
 * Java would normally default a primitive {@code int} to {@code 0}, masking the null intent.
 * To handle this correctly:
 *
 * <ol>
 *   <li>All numeric fields use <em>boxed</em> types ({@code Integer}, {@code Double})
 *       so Jackson maps JSON null → Java {@code null}, not {@code 0}.</li>
 *   <li>All boolean fields use <em>boxed</em> {@code Boolean} so JSON null → Java {@code null}.</li>
 *   <li>{@link #resolveDefaults()} is called in the service layer to apply
 *       sensible defaults before persisting.</li>
 * </ol>
 *
 * <p>This approach distinguishes between:
 * <ul>
 *   <li>"Client explicitly sent null" → we apply our default</li>
 *   <li>"Client sent 0 or false" → we preserve the value</li>
 *   <li>"Client omitted the field" → treated same as null</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductRequest {

    private String name;
    private String category;
    private String description;

    /**
     * Nullable Integer — null means "not provided".
     * Service layer will default to 0 if null.
     */
    private Integer price;

    /**
     * Nullable Integer — null means "not provided".
     * Service layer will default to 0 if null.
     */
    private Integer stockCount;

    /**
     * Nullable Integer — null is valid (unrated product).
     * No default applied.
     */
    private Integer rating;

    /**
     * Nullable Boolean — null means "not provided".
     * Service layer will default to true if null (new products are active by default).
     */
    private Boolean active;

    /**
     * Nullable Boolean — null means "not provided".
     * Service layer will default to false if null.
     */
    private Boolean featured;

    /**
     * Nullable Boolean — null means "not provided".
     * Derived from stockCount if null.
     */
    private Boolean inStock;

    /**
     * Applies default values for null/empty int and boolean fields.
     *
     * <p>This method is the single source of truth for business defaults,
     * keeping that logic out of controllers and out of the domain model.
     *
     * @return this instance (fluent)
     */
    public ProductRequest resolveDefaults() {
        // Numeric defaults
        if (price == null) {
            price = 0;
        }
        if (stockCount == null) {
            stockCount = 0;
        }
        // rating intentionally left null — no rating is valid

        // Boolean defaults
        if (active == null) {
            active = Boolean.TRUE;  // new products active by default
        }
        if (featured == null) {
            featured = Boolean.FALSE;
        }
        // Derive inStock from stockCount if not explicitly set
        if (inStock == null) {
            inStock = stockCount > 0;
        }

        return this;
    }

    /**
     * Validates that the request contains at least the required fields.
     * @throws IllegalArgumentException if name or category is missing
     */
    public void validate() {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Product category is required");
        }
    }
}
