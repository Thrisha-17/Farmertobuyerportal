package com.farmdirect.dto;

import com.farmdirect.model.Product;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ProductDtos {

    public static class ProductRequest {
        @NotBlank public String name;
        @NotBlank public String category;
        public String description;
        @NotNull @DecimalMin("0.01") public BigDecimal pricePerUnit;
        @NotBlank public String unit;
        @NotNull @Min(1) public Integer quantityAvailable;
        public LocalDate harvestDate;
        public String imageUrl;
    }

    public static class ProductResponse {
        public Long id;
        public String name;
        public String category;
        public String description;
        public BigDecimal pricePerUnit;
        public String unit;
        public Integer quantityAvailable;
        public LocalDate harvestDate;
        public String imageUrl;
        public Long farmerId;
        public String farmerName;
        public String farmerLocation;
        public String farmerPhone;

        public static ProductResponse from(Product p) {
            ProductResponse r = new ProductResponse();
            r.id = p.getId();
            r.name = p.getName();
            r.category = p.getCategory();
            r.description = p.getDescription();
            r.pricePerUnit = p.getPricePerUnit();
            r.unit = p.getUnit();
            r.quantityAvailable = p.getQuantityAvailable();
            r.harvestDate = p.getHarvestDate();
            r.imageUrl = p.getImageUrl();
            if (p.getFarmer() != null) {
                r.farmerId = p.getFarmer().getId();
                r.farmerName = p.getFarmer().getName();
                r.farmerLocation = p.getFarmer().getLocation();
                r.farmerPhone = p.getFarmer().getPhone();
            }
            return r;
        }
    }
}
