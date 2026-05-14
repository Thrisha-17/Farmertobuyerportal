package com.farmdirect.service;

import com.farmdirect.dto.OrderDtos.*;
import com.farmdirect.model.Order;
import com.farmdirect.model.Product;
import com.farmdirect.model.User;
import com.farmdirect.repository.OrderRepository;
import com.farmdirect.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse place(OrderRequest req, User buyer) {
        Product p = productRepository.findById(req.productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (p.getQuantityAvailable() < req.quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough stock available");
        }
        if (p.getFarmer().getId().equals(buyer.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot order your own product");
        }
        Order o = new Order();
        o.setBuyer(buyer);
        o.setProduct(p);
        o.setQuantity(req.quantity);
        o.setTotalPrice(p.getPricePerUnit().multiply(BigDecimal.valueOf(req.quantity)));
        o.setStatus(Order.Status.PENDING);
        o.setDeliveryAddress(req.deliveryAddress);
        o.setNotes(req.notes);
        p.setQuantityAvailable(p.getQuantityAvailable() - req.quantity);
        productRepository.save(p);
        return OrderResponse.from(orderRepository.save(o));
    }

    public List<OrderResponse> forBuyer(User buyer) {
        return orderRepository.findByBuyerOrderByCreatedAtDesc(buyer)
                .stream().map(OrderResponse::from).toList();
    }

    public List<OrderResponse> forFarmer(User farmer) {
        return orderRepository.findByFarmer(farmer)
                .stream().map(OrderResponse::from).toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long id, String status, User farmer) {
        Order o = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!o.getProduct().getFarmer().getId().equals(farmer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        }
        Order.Status newStatus;
        try {
            newStatus = Order.Status.valueOf(status.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        Order.Status previous = o.getStatus();
        // Restore stock if the order is being cancelled for the first time.
        if (newStatus == Order.Status.CANCELLED && previous != Order.Status.CANCELLED) {
            Product p = o.getProduct();
            p.setQuantityAvailable(p.getQuantityAvailable() + o.getQuantity());
            productRepository.save(p);
        }
        o.setStatus(newStatus);
        return OrderResponse.from(orderRepository.save(o));
    }
}
