package com.farmdirect.dto;

import com.farmdirect.model.Order;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderDtos {

    public static class OrderRequest {
        @NotNull public Long productId;
        @NotNull @Min(1) public Integer quantity;
        @NotBlank public String deliveryAddress;
        public String notes;
    }

    public static class StatusRequest {
        @NotBlank public String status;
    }

    public static class OrderResponse {
        public Long id;
        public Long productId;
        public String productName;
        public String productImage;
        public String productUnit;
        public Long buyerId;
        public String buyerName;
        public String buyerPhone;
        public Long farmerId;
        public String farmerName;
        public String farmerPhone;
        public Integer quantity;
        public BigDecimal totalPrice;
        public String status;
        public String deliveryAddress;
        public String notes;
        public LocalDateTime createdAt;

        public static OrderResponse from(Order o) {
            OrderResponse r = new OrderResponse();
            r.id = o.getId();
            r.quantity = o.getQuantity();
            r.totalPrice = o.getTotalPrice();
            r.status = o.getStatus().name();
            r.deliveryAddress = o.getDeliveryAddress();
            r.notes = o.getNotes();
            r.createdAt = o.getCreatedAt();
            if (o.getProduct() != null) {
                r.productId = o.getProduct().getId();
                r.productName = o.getProduct().getName();
                r.productImage = o.getProduct().getImageUrl();
                r.productUnit = o.getProduct().getUnit();
                if (o.getProduct().getFarmer() != null) {
                    r.farmerId = o.getProduct().getFarmer().getId();
                    r.farmerName = o.getProduct().getFarmer().getName();
                    r.farmerPhone = o.getProduct().getFarmer().getPhone();
                }
            }
            if (o.getBuyer() != null) {
                r.buyerId = o.getBuyer().getId();
                r.buyerName = o.getBuyer().getName();
                r.buyerPhone = o.getBuyer().getPhone();
            }
            return r;
        }
    }
}
