// ============================================================
// FARMCONNECT – Spring Boot Backend (Java)
// Full Stack: HTML/CSS/JS + Java Spring Boot + MySQL
// ============================================================

// ── 1. pom.xml dependencies ─────────────────────────────────
/*
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
  </dependency>
  <dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
  </dependency>
</dependencies>
*/

// ── 2. application.properties ────────────────────────────────
/*
spring.datasource.url=jdbc:mysql://localhost:3306/farmconnect
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
jwt.secret=farmconnect_secret_key_2026
jwt.expiration=86400000
*/

// ── 3. DATABASE SCHEMA (MySQL) ───────────────────────────────
/*
CREATE DATABASE farmconnect;
USE farmconnect;

-- USERS TABLE (shared for Farmer & Buyer)
CREATE TABLE users (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  first_name  VARCHAR(100) NOT NULL,
  last_name   VARCHAR(100) NOT NULL,
  email       VARCHAR(255) NOT NULL UNIQUE,
  mobile      VARCHAR(20)  NOT NULL UNIQUE,
  password    VARCHAR(255) NOT NULL,
  role        ENUM('FARMER','BUYER') NOT NULL,
  state       VARCHAR(100),
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- FARMER PROFILES
CREATE TABLE farmer_profiles (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT NOT NULL REFERENCES users(id),
  farm_name    VARCHAR(200),
  farm_size    DECIMAL(10,2),
  crop_type    VARCHAR(100),
  village      VARCHAR(200),
  about        TEXT,
  rating       DECIMAL(3,2) DEFAULT 0.0,
  is_verified  BOOLEAN DEFAULT FALSE
);

-- BUYER PROFILES
CREATE TABLE buyer_profiles (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id),
  buyer_type  VARCHAR(100),
  address     TEXT
);

-- PRODUCTS (Farmer sets price directly)
CREATE TABLE products (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  farmer_id      BIGINT NOT NULL REFERENCES users(id),
  name           VARCHAR(200) NOT NULL,
  description    TEXT,
  category       VARCHAR(100) NOT NULL,
  price          DECIMAL(10,2) NOT NULL,  -- Set ONLY by farmer
  unit           VARCHAR(50)  NOT NULL,
  stock_quantity DECIMAL(10,2) NOT NULL,
  is_organic     BOOLEAN DEFAULT FALSE,
  harvest_date   DATE,
  is_active      BOOLEAN DEFAULT TRUE,
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ORDERS (Direct Farmer–Buyer, no middleman)
CREATE TABLE orders (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  buyer_id        BIGINT NOT NULL REFERENCES users(id),
  farmer_id       BIGINT NOT NULL REFERENCES users(id),
  total_amount    DECIMAL(10,2) NOT NULL,
  delivery_method ENUM('FARM_PICKUP','LOCAL_DELIVERY','COURIER','COOPERATIVE_HUB','SUBSCRIPTION','COLD_CHAIN'),
  delivery_fee    DECIMAL(10,2) DEFAULT 0.00,
  delivery_address TEXT,
  status          ENUM('PENDING','CONFIRMED','DISPATCHED','DELIVERED','CANCELLED') DEFAULT 'PENDING',
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ORDER ITEMS
CREATE TABLE order_items (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id   BIGINT NOT NULL REFERENCES orders(id),
  product_id BIGINT NOT NULL REFERENCES products(id),
  quantity   DECIMAL(10,2) NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL  -- Locked price at time of order
);

-- DELIVERY OPTIONS (per farmer)
CREATE TABLE farmer_delivery_options (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  farmer_id       BIGINT NOT NULL REFERENCES users(id),
  delivery_type   VARCHAR(100) NOT NULL,
  is_enabled      BOOLEAN DEFAULT FALSE,
  delivery_fee    DECIMAL(10,2) DEFAULT 0.00,
  max_distance_km INT
);
*/

// =====================================================================
// PACKAGE: com.farmconnect
// =====================================================================

// ── FarmConnectApplication.java ──────────────────────────────
package com.farmconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FarmConnectApplication {
    public static void main(String[] args) {
        SpringApplication.run(FarmConnectApplication.class, args);
    }
}

// =====================================================================
// MODELS (Entity classes)
// =====================================================================

// ── model/User.java ───────────────────────────────────────────
package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String mobile;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String state;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Role { FARMER, BUYER }
}

// ── model/Product.java ────────────────────────────────────────
package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String category;

    // Price is set ONLY by the farmer — no intermediary markup allowed
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private BigDecimal stockQuantity;

    private Boolean isOrganic = false;
    private LocalDate harvestDate;
    private Boolean isActive = true;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}

// ── model/Order.java ──────────────────────────────────────────
package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id")
    private User farmer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod;

    private BigDecimal deliveryFee;
    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum DeliveryMethod {
        FARM_PICKUP, LOCAL_DELIVERY, COURIER, COOPERATIVE_HUB, SUBSCRIPTION, COLD_CHAIN
    }

    public enum OrderStatus {
        PENDING, CONFIRMED, DISPATCHED, DELIVERED, CANCELLED
    }
}

// ── model/OrderItem.java ──────────────────────────────────────
package com.farmconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private BigDecimal quantity;
    private BigDecimal unitPrice;  // Locked at time of order
}

// =====================================================================
// DTOs
// =====================================================================

// ── dto/RegisterRequest.java ──────────────────────────────────
package com.farmconnect.dto;

import com.farmconnect.model.User;
import lombok.Data;

@Data
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String mobile;
    private String password;
    private User.Role role;
    private String state;
    // Farmer fields
    private Double farmSize;
    private String cropType;
    // Buyer fields
    private String buyerType;
}

// ── dto/LoginRequest.java ─────────────────────────────────────
package com.farmconnect.dto;
import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
    private String role;
}

// ── dto/AuthResponse.java ─────────────────────────────────────
package com.farmconnect.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class AuthResponse {
    private String token;
    private String role;
    private String name;
    private Long userId;
}

// ── dto/ProductRequest.java ───────────────────────────────────
package com.farmconnect.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductRequest {
    private String name;
    private String description;
    private String category;
    private BigDecimal price;      // Farmer-controlled price
    private String unit;
    private BigDecimal stockQuantity;
    private Boolean isOrganic;
    private LocalDate harvestDate;
}

// ── dto/OrderRequest.java ─────────────────────────────────────
package com.farmconnect.dto;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private Long farmerId;
    private List<OrderItemRequest> items;
    private String deliveryMethod;
    private String deliveryAddress;

    @Data
    public static class OrderItemRequest {
        private Long productId;
        private Double quantity;
    }
}

// =====================================================================
// REPOSITORIES
// =====================================================================

// ── repository/UserRepository.java ───────────────────────────
package com.farmconnect.repository;

import com.farmconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
}

// ── repository/ProductRepository.java ────────────────────────
package com.farmconnect.repository;

import com.farmconnect.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByFarmerIdAndIsActiveTrue(Long farmerId);
    List<Product> findByCategoryAndIsActiveTrue(String category);
    List<Product> findByIsActiveTrue();
    List<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
}

// ── repository/OrderRepository.java ──────────────────────────
package com.farmconnect.repository;

import com.farmconnect.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    List<Order> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
    List<Order> findByFarmerIdAndStatus(Long farmerId, Order.OrderStatus status);
}

// =====================================================================
// SERVICES
// =====================================================================

// ── service/AuthService.java ──────────────────────────────────
package com.farmconnect.service;

import com.farmconnect.dto.*;
import com.farmconnect.model.User;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email already registered");

        if (userRepository.existsByMobile(request.getMobile()))
            throw new RuntimeException("Mobile number already registered");

        User user = User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .mobile(request.getMobile())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(request.getRole())
            .state(request.getState())
            .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getRole().name(),
            user.getFirstName() + " " + user.getLastName(), user.getId());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid credentials");

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getRole().name(),
            user.getFirstName() + " " + user.getLastName(), user.getId());
    }
}

// ── service/ProductService.java ───────────────────────────────
package com.farmconnect.service;

import com.farmconnect.dto.ProductRequest;
import com.farmconnect.model.Product;
import com.farmconnect.model.User;
import com.farmconnect.repository.ProductRepository;
import com.farmconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // FARMER: Add product with self-set price (no intermediary)
    public Product addProduct(Long farmerId, ProductRequest request) {
        User farmer = userRepository.findById(farmerId)
            .orElseThrow(() -> new RuntimeException("Farmer not found"));

        if (farmer.getRole() != User.Role.FARMER)
            throw new RuntimeException("Only farmers can add products");

        Product product = Product.builder()
            .farmer(farmer)
            .name(request.getName())
            .description(request.getDescription())
            .category(request.getCategory())
            .price(request.getPrice())         // Farmer-set price
            .unit(request.getUnit())
            .stockQuantity(request.getStockQuantity())
            .isOrganic(request.getIsOrganic())
            .harvestDate(request.getHarvestDate())
            .isActive(true)
            .build();

        return productRepository.save(product);
    }

    // FARMER: Update product (only the owning farmer can edit)
    public Product updateProduct(Long farmerId, Long productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getFarmer().getId().equals(farmerId))
            throw new RuntimeException("Unauthorized: You can only edit your own products");

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setUpdatedAt(LocalDateTime.now());

        return productRepository.save(product);
    }

    // FARMER: Delete/deactivate product
    public void deactivateProduct(Long farmerId, Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getFarmer().getId().equals(farmerId))
            throw new RuntimeException("Unauthorized");

        product.setIsActive(false);
        productRepository.save(product);
    }

    // BUYER: Browse all active products
    public List<Product> getAllActiveProducts() {
        return productRepository.findByIsActiveTrue();
    }

    // BUYER: Filter by category
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndIsActiveTrue(category);
    }

    // BUYER: Search products
    public List<Product> searchProducts(String query) {
        return productRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(query);
    }

    // FARMER: My products
    public List<Product> getFarmerProducts(Long farmerId) {
        return productRepository.findByFarmerIdAndIsActiveTrue(farmerId);
    }
}

// ── service/OrderService.java ─────────────────────────────────
package com.farmconnect.service;

import com.farmconnect.dto.OrderRequest;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Order placeOrder(Long buyerId, OrderRequest request) {
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new RuntimeException("Buyer not found"));

        User farmer = userRepository.findById(request.getFarmerId())
            .orElseThrow(() -> new RuntimeException("Farmer not found"));

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

            // Validate stock
            BigDecimal qty = BigDecimal.valueOf(itemReq.getQuantity());
            if (product.getStockQuantity().compareTo(qty) < 0)
                throw new RuntimeException("Insufficient stock for: " + product.getName());

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity().subtract(qty));
            productRepository.save(product);

            // Lock price at order time (farmer's price, no markup)
            OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(qty)
                .unitPrice(product.getPrice())
                .build();

            total = total.add(product.getPrice().multiply(qty));
            items.add(item);
        }

        // Calculate delivery fee
        BigDecimal deliveryFee = calculateDeliveryFee(request.getDeliveryMethod());

        Order order = Order.builder()
            .buyer(buyer)
            .farmer(farmer)
            .items(items)
            .totalAmount(total.add(deliveryFee))
            .deliveryMethod(Order.DeliveryMethod.valueOf(request.getDeliveryMethod()))
            .deliveryFee(deliveryFee)
            .deliveryAddress(request.getDeliveryAddress())
            .status(Order.OrderStatus.PENDING)
            .build();

        items.forEach(i -> i.setOrder(order));
        return orderRepository.save(order);
    }

    // FARMER: Update order status
    public Order updateOrderStatus(Long farmerId, Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getFarmer().getId().equals(farmerId))
            throw new RuntimeException("Unauthorized");

        order.setStatus(Order.OrderStatus.valueOf(status.toUpperCase()));
        return orderRepository.save(order);
    }

    public List<Order> getBuyerOrders(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    public List<Order> getFarmerOrders(Long farmerId) {
        return orderRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId);
    }

    private BigDecimal calculateDeliveryFee(String method) {
        return switch (method) {
            case "FARM_PICKUP"      -> BigDecimal.ZERO;
            case "LOCAL_DELIVERY"   -> BigDecimal.valueOf(50);
            case "COURIER"          -> BigDecimal.valueOf(120);
            case "COOPERATIVE_HUB" -> BigDecimal.valueOf(40);
            case "SUBSCRIPTION"     -> BigDecimal.valueOf(30);
            case "COLD_CHAIN"       -> BigDecimal.valueOf(200);
            default -> BigDecimal.ZERO;
        };
    }
}

// =====================================================================
// CONTROLLERS
// =====================================================================

// ── controller/AuthController.java ───────────────────────────
package com.farmconnect.controller;

import com.farmconnect.dto.*;
import com.farmconnect.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

// ── controller/ProductController.java ────────────────────────
package com.farmconnect.controller;

import com.farmconnect.dto.ProductRequest;
import com.farmconnect.model.Product;
import com.farmconnect.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // BUYER: Browse market
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        if (search != null) return ResponseEntity.ok(productService.searchProducts(search));
        if (category != null) return ResponseEntity.ok(productService.getProductsByCategory(category));
        return ResponseEntity.ok(productService.getAllActiveProducts());
    }

    // FARMER: Add product (farmer sets price)
    @PostMapping("/farmer/{farmerId}")
    public ResponseEntity<Product> addProduct(
            @PathVariable Long farmerId,
            @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.addProduct(farmerId, request));
    }

    // FARMER: Update own product
    @PutMapping("/farmer/{farmerId}/{productId}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long farmerId,
            @PathVariable Long productId,
            @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(farmerId, productId, request));
    }

    // FARMER: Deactivate product
    @DeleteMapping("/farmer/{farmerId}/{productId}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Long farmerId,
            @PathVariable Long productId) {
        productService.deactivateProduct(farmerId, productId);
        return ResponseEntity.ok("Product deactivated");
    }

    // FARMER: My product listings
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<Product>> getFarmerProducts(@PathVariable Long farmerId) {
        return ResponseEntity.ok(productService.getFarmerProducts(farmerId));
    }
}

// ── controller/OrderController.java ──────────────────────────
package com.farmconnect.controller;

import com.farmconnect.dto.OrderRequest;
import com.farmconnect.model.Order;
import com.farmconnect.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // BUYER: Place order directly with farmer
    @PostMapping("/buyer/{buyerId}")
    public ResponseEntity<Order> placeOrder(
            @PathVariable Long buyerId,
            @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(buyerId, request));
    }

    // BUYER: My orders
    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<Order>> getBuyerOrders(@PathVariable Long buyerId) {
        return ResponseEntity.ok(orderService.getBuyerOrders(buyerId));
    }

    // FARMER: Incoming orders
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<Order>> getFarmerOrders(@PathVariable Long farmerId) {
        return ResponseEntity.ok(orderService.getFarmerOrders(farmerId));
    }

    // FARMER: Update order status
    @PatchMapping("/farmer/{farmerId}/{orderId}/status")
    public ResponseEntity<Order> updateStatus(
            @PathVariable Long farmerId,
            @PathVariable Long orderId,
            @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(farmerId, orderId, status));
    }
}

// =====================================================================
// SECURITY – JWT
// =====================================================================

// ── security/JwtUtil.java ─────────────────────────────────────
package com.farmconnect.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
            .setSubject(email)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(getKey()).build()
            .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}

// =====================================================================
// REST API REFERENCE
// =====================================================================
/*
POST   /api/auth/register              → Register farmer or buyer
POST   /api/auth/login                 → Login, returns JWT token

GET    /api/products                   → Browse all products (buyer)
GET    /api/products?category=Fruits   → Filter by category
GET    /api/products?search=tomato     → Search products
GET    /api/products/farmer/{id}       → Farmer's own listings
POST   /api/products/farmer/{id}       → Add product (farmer only)
PUT    /api/products/farmer/{id}/{pid} → Update product + price
DELETE /api/products/farmer/{id}/{pid} → Remove product

POST   /api/orders/buyer/{id}          → Place direct order
GET    /api/orders/buyer/{id}          → Buyer's order history
GET    /api/orders/farmer/{id}         → Farmer's incoming orders
PATCH  /api/orders/farmer/{id}/{oid}/status → Update order status

DELIVERY METHODS:
  FARM_PICKUP      → ₹0    – Buyer collects from farm
  LOCAL_DELIVERY   → ₹50   – Farmer delivers within 20km
  COURIER          → ₹120  – Third-party (Delhivery/DTDC)
  COOPERATIVE_HUB  → ₹40   – Drop at collection point
  SUBSCRIPTION     → ₹30   – Weekly delivery box
  COLD_CHAIN       → ₹200  – Temperature-controlled transport
*/
