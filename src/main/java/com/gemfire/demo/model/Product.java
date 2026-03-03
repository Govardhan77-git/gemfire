package com.gemfire.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxSerializable;
import org.apache.geode.pdx.PdxWriter;

import java.io.Serializable;
import java.time.Instant;

/**
 * Product domain model stored in GemFire.
 *
 * NULL/Empty Handling Strategy:
 *   - Integer (boxed) instead of int  → allows null from JSON
 *   - Boolean (boxed) instead of boolean → allows null from JSON
 *   - PDX writeObject used for boxed types to preserve null semantics
 *
 * Implements PdxSerializable for GemFire PDX (Portable Data eXchange) —
 * cross-language and schema-evolution safe serialization.
 *
 * Note: Lombok @Data removed — conflicts with PdxSerializable when combined
 * with @Builder. Manual getters/setters used instead for reliability.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Product implements Serializable, PdxSerializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String category;
    private String description;

    /** Boxed Integer — null means "price not set", defaults to 0 */
    private Integer price;

    /** Boxed Integer — null means "stock not set", defaults to 0 */
    private Integer stockCount;

    /** Boxed Integer — null is valid (product may not be rated yet) */
    private Integer rating;

    /** Boxed Boolean — null means "not set", defaults to true (active) */
    private Boolean active;

    /** Boxed Boolean — null means "not set", defaults to false */
    private Boolean featured;

    /** Boxed Boolean — derived from stockCount when null */
    private Boolean inStock;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;

    public Product() {}

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Product p = new Product();
        public Builder id(String v)          { p.id = v; return this; }
        public Builder name(String v)        { p.name = v; return this; }
        public Builder category(String v)    { p.category = v; return this; }
        public Builder description(String v) { p.description = v; return this; }
        public Builder price(Integer v)      { p.price = v; return this; }
        public Builder price(int v)          { p.price = v; return this; }
        public Builder stockCount(Integer v) { p.stockCount = v; return this; }
        public Builder stockCount(int v)     { p.stockCount = v; return this; }
        public Builder rating(Integer v)     { p.rating = v; return this; }
        public Builder active(Boolean v)     { p.active = v; return this; }
        public Builder active(boolean v)     { p.active = v; return this; }
        public Builder featured(Boolean v)   { p.featured = v; return this; }
        public Builder featured(boolean v)   { p.featured = v; return this; }
        public Builder inStock(Boolean v)    { p.inStock = v; return this; }
        public Builder inStock(boolean v)    { p.inStock = v; return this; }
        public Builder createdAt(Instant v)  { p.createdAt = v; return this; }
        public Builder updatedAt(Instant v)  { p.updatedAt = v; return this; }
        public Builder createdBy(String v)   { p.createdBy = v; return this; }
        public Product build()               { return p; }
    }

    // ── PDX Serialization ─────────────────────────────────────────────────────

    @Override
    public void toData(PdxWriter writer) {
        writer.writeString("id", id);
        writer.writeString("name", name);
        writer.writeString("category", category);
        writer.writeString("description", description);
        writer.writeObject("price", price);
        writer.writeObject("stockCount", stockCount);
        writer.writeObject("rating", rating);
        writer.writeObject("active", active);
        writer.writeObject("featured", featured);
        writer.writeObject("inStock", inStock);
        writer.writeString("createdBy", createdBy);
        writer.writeObject("createdAt", createdAt);
        writer.writeObject("updatedAt", updatedAt);
    }

    @Override
    public void fromData(PdxReader reader) {
        id          = reader.readString("id");
        name        = reader.readString("name");
        category    = reader.readString("category");
        description = reader.readString("description");
        price       = (Integer) reader.readObject("price");
        stockCount  = (Integer) reader.readObject("stockCount");
        rating      = (Integer) reader.readObject("rating");
        active      = (Boolean) reader.readObject("active");
        featured    = (Boolean) reader.readObject("featured");
        inStock     = (Boolean) reader.readObject("inStock");
        createdBy   = reader.readString("createdBy");
        createdAt   = (Instant) reader.readObject("createdAt");
        updatedAt   = (Instant) reader.readObject("updatedAt");
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()            { return id; }
    public void setId(String id)     { this.id = id; }

    public String getName()               { return name; }
    public void setName(String name)      { this.name = name; }

    public String getCategory()                { return category; }
    public void setCategory(String category)   { this.category = category; }

    public String getDescription()                  { return description; }
    public void setDescription(String description)  { this.description = description; }

    public Integer getPrice()              { return price; }
    public void setPrice(Integer price)    { this.price = price; }

    public Integer getStockCount()                  { return stockCount; }
    public void setStockCount(Integer stockCount)   { this.stockCount = stockCount; }

    public Integer getRating()               { return rating; }
    public void setRating(Integer rating)    { this.rating = rating; }

    public Boolean getActive()               { return active; }
    public void setActive(Boolean active)    { this.active = active; }

    public Boolean getFeatured()                { return featured; }
    public void setFeatured(Boolean featured)   { this.featured = featured; }

    public Boolean getInStock()               { return inStock; }
    public void setInStock(Boolean inStock)   { this.inStock = inStock; }

    public Instant getCreatedAt()                { return createdAt; }
    public void setCreatedAt(Instant createdAt)  { this.createdAt = createdAt; }

    public Instant getUpdatedAt()                { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt)  { this.updatedAt = updatedAt; }

    public String getCreatedBy()                { return createdBy; }
    public void setCreatedBy(String createdBy)  { this.createdBy = createdBy; }

    @Override
    public String toString() {
        return "Product{id='" + id + "', name='" + name + "', category='" + category +
               "', price=" + price + ", active=" + active + ", inStock=" + inStock + "}";
    }
}
