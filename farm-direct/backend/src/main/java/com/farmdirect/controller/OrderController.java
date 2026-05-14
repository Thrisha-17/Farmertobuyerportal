package com.farmdirect.controller;

import com.farmdirect.dto.OrderDtos.*;
import com.farmdirect.model.User;
import com.farmdirect.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public OrderResponse place(@Valid @RequestBody OrderRequest req,
                               @AuthenticationPrincipal User user) {
        return orderService.place(req, user);
    }

    @GetMapping("/buyer")
    @PreAuthorize("hasRole('BUYER')")
    public List<OrderResponse> buyerOrders(@AuthenticationPrincipal User user) {
        return orderService.forBuyer(user);
    }

    @GetMapping("/farmer")
    @PreAuthorize("hasRole('FARMER')")
    public List<OrderResponse> farmerOrders(@AuthenticationPrincipal User user) {
        return orderService.forFarmer(user);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('FARMER')")
    public OrderResponse updateStatus(@PathVariable Long id,
                                      @Valid @RequestBody StatusRequest req,
                                      @AuthenticationPrincipal User user) {
        return orderService.updateStatus(id, req.status, user);
    }
}
