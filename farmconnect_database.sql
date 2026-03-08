-- ===================================================================
-- FarmConnect – Complete MySQL Database
-- Run this file to set up the entire database from scratch
-- ===================================================================

CREATE DATABASE IF NOT EXISTS farmconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE farmconnect;

-- ===================================================================
-- TABLE 1: USERS (shared for Farmer & Buyer)
-- ===================================================================
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    first_name  VARCHAR(100)    NOT NULL,
    last_name   VARCHAR(100)    NOT NULL,
    email       VARCHAR(255)    NOT NULL UNIQUE,
    mobile      VARCHAR(20)     NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,       -- BCrypt hashed
    role        ENUM('FARMER','BUYER') NOT NULL,
    state       VARCHAR(100),
    is_active   BOOLEAN         DEFAULT TRUE,
    created_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role  (role)
);

-- ===================================================================
-- TABLE 2: FARMER PROFILES
-- ===================================================================
CREATE TABLE IF NOT EXISTS farmer_profiles (
    id           BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT          NOT NULL UNIQUE,
    farm_name    VARCHAR(200),
    farm_size    DECIMAL(10,2),                  -- in acres
    crop_type    VARCHAR(100),
    village      VARCHAR(200),
    about        TEXT,
    rating       DECIMAL(3,2)    DEFAULT 0.0,
    total_ratings INT            DEFAULT 0,
    is_verified  BOOLEAN         DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ===================================================================
-- TABLE 3: BUYER PROFILES
-- ===================================================================
CREATE TABLE IF NOT EXISTS buyer_profiles (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT          NOT NULL UNIQUE,
    buyer_type  VARCHAR(100),                    -- Individual / Restaurant / Grocery / Bulk
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ===================================================================
-- TABLE 4: DELIVERY ADDRESSES
-- ===================================================================
CREATE TABLE IF NOT EXISTS addresses (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT          NOT NULL,
    label       VARCHAR(50)     DEFAULT 'Home', -- Home / Office / Other
    street      TEXT            NOT NULL,
    city        VARCHAR(100)    NOT NULL,
    state       VARCHAR(100)    NOT NULL,
    pincode     VARCHAR(10)     NOT NULL,
    is_default  BOOLEAN         DEFAULT FALSE,
    created_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_address (user_id)
);

-- ===================================================================
-- TABLE 5: PRODUCTS
-- Farmer sets the price — NO intermediary can change it
-- ===================================================================
CREATE TABLE IF NOT EXISTS products (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    farmer_id       BIGINT          NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    description     TEXT,
    category        VARCHAR(100)    NOT NULL,   -- Vegetables / Fruits / Grains / Dairy / Spices / Pulses
    price           DECIMAL(10,2)   NOT NULL,   -- *** SET ONLY BY FARMER – NEVER BY INTERMEDIARY ***
    unit            VARCHAR(50)     NOT NULL,   -- per kg / per dozen / per litre / per piece
    stock_quantity  DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    is_organic      BOOLEAN         DEFAULT FALSE,
    harvest_date    DATE,
    image_url       VARCHAR(500),
    is_active       BOOLEAN         DEFAULT TRUE,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (farmer_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_farmer_products (farmer_id),
    INDEX idx_category        (category),
    INDEX idx_active          (is_active),
    FULLTEXT INDEX ft_search  (name, description)
);

-- ===================================================================
-- TABLE 6: FARMER DELIVERY OPTIONS
-- Each farmer configures which delivery methods they support
-- ===================================================================
CREATE TABLE IF NOT EXISTS farmer_delivery_options (
    id             BIGINT          AUTO_INCREMENT PRIMARY KEY,
    farmer_id      BIGINT          NOT NULL,
    delivery_type  VARCHAR(50)     NOT NULL,   -- FARM_PICKUP / LOCAL_DELIVERY / COURIER / COOPERATIVE_HUB / SUBSCRIPTION / COLD_CHAIN
    is_enabled     BOOLEAN         DEFAULT FALSE,
    delivery_fee   DECIMAL(10,2)   DEFAULT 0.00,
    max_distance   INT             DEFAULT 0,   -- km (for LOCAL_DELIVERY)
    notes          VARCHAR(300),
    UNIQUE KEY uq_farmer_delivery_type (farmer_id, delivery_type),
    FOREIGN KEY (farmer_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ===================================================================
-- TABLE 7: ORDERS (Direct Farmer ↔ Buyer — NO MIDDLEMEN)
-- ===================================================================
CREATE TABLE IF NOT EXISTS orders (
    id               BIGINT          AUTO_INCREMENT PRIMARY KEY,
    order_number     VARCHAR(20)     NOT NULL UNIQUE,
    buyer_id         BIGINT          NOT NULL,
    farmer_id        BIGINT          NOT NULL,
    subtotal         DECIMAL(10,2)   NOT NULL,
    delivery_fee     DECIMAL(10,2)   DEFAULT 0.00,
    total_amount     DECIMAL(10,2)   NOT NULL,
    delivery_method  VARCHAR(50)     NOT NULL,
    delivery_address TEXT,
    notes            TEXT,
    status           ENUM('PENDING','CONFIRMED','DISPATCHED','DELIVERED','CANCELLED') DEFAULT 'PENDING',
    created_at       TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (buyer_id)  REFERENCES users(id),
    FOREIGN KEY (farmer_id) REFERENCES users(id),
    INDEX idx_buyer_orders  (buyer_id),
    INDEX idx_farmer_orders (farmer_id),
    INDEX idx_order_status  (status)
);

-- ===================================================================
-- TABLE 8: ORDER ITEMS
-- Farmer price is LOCKED at the time of order (unit_price)
-- ===================================================================
CREATE TABLE IF NOT EXISTS order_items (
    id           BIGINT          AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT          NOT NULL,
    product_id   BIGINT          NOT NULL,
    product_name VARCHAR(200)    NOT NULL,      -- Snapshot at order time
    quantity     DECIMAL(10,2)   NOT NULL,
    unit         VARCHAR(50)     NOT NULL,
    unit_price   DECIMAL(10,2)   NOT NULL,      -- *** LOCKED FARMER PRICE – NEVER CHANGES ***
    line_total   DECIMAL(10,2)   NOT NULL,
    FOREIGN KEY (order_id)   REFERENCES orders(id)  ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_order_items (order_id)
);

-- ===================================================================
-- TABLE 9: RATINGS
-- Buyers rate farmers after delivery
-- ===================================================================
CREATE TABLE IF NOT EXISTS ratings (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT          NOT NULL UNIQUE,  -- One rating per order
    buyer_id    BIGINT          NOT NULL,
    farmer_id   BIGINT          NOT NULL,
    stars       TINYINT         NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id)  REFERENCES orders(id),
    FOREIGN KEY (buyer_id)  REFERENCES users(id),
    FOREIGN KEY (farmer_id) REFERENCES users(id),
    INDEX idx_farmer_ratings (farmer_id)
);

-- ===================================================================
-- SAMPLE DATA (for testing)
-- ===================================================================

-- Sample Farmer (password: password123)
INSERT IGNORE INTO users (first_name,last_name,email,mobile,password,role,state) VALUES
('Ravi','Kumar','ravi@farm.com','9876543210','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TsDW0hk7k6hk6hk6hk6hk6hk6hk6','FARMER','Karnataka'),
('Suresh','Patil','suresh@farm.com','9876543211','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TsDW0hk7k6hk6hk6hk6hk6hk6hk6','FARMER','Maharashtra');

-- Sample Buyer (password: password123)
INSERT IGNORE INTO users (first_name,last_name,email,mobile,password,role,state) VALUES
('Priya','Reddy','priya@buyer.com','9876543220','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TsDW0hk7k6hk6hk6hk6hk6hk6hk6','BUYER','Karnataka');

-- Sample Farmer Profiles
INSERT IGNORE INTO farmer_profiles (user_id,farm_size,crop_type,village,about,rating,total_ratings) VALUES
(1, 8.5, 'Vegetables', 'Kolar District', 'Organic farming using traditional methods, no pesticides.', 4.8, 24),
(2, 12.0, 'Fruits', 'Ratnagiri', 'Alphonso mango specialist, 3rd generation farmer.', 4.9, 18);

-- Sample Buyer Profile
INSERT IGNORE INTO buyer_profiles (user_id,buyer_type) VALUES
(3,'Individual / Family');

-- Sample Products (for Farmer id=1)
INSERT IGNORE INTO products (farmer_id,name,description,category,price,unit,stock_quantity,is_organic,harvest_date) VALUES
(1,'Fresh Tomatoes','Freshly harvested, no pesticides, deep red variety','Vegetables',65.00,'per kg',150.00,TRUE,'2026-03-04'),
(1,'Red Onions','Premium quality Nasik onions, shelf life 3 weeks','Vegetables',45.00,'per kg',300.00,FALSE,NULL),
(1,'Green Chilli','Spicy Byadagi variety, freshly picked','Spices',80.00,'per kg',40.00,TRUE,'2026-03-05'),
(2,'Alphonso Mangoes','King of mangoes – GI tagged Ratnagiri Alphonso','Fruits',280.00,'per dozen',80.00,TRUE,'2026-03-01'),
(2,'Coconut (Tender)','Fresh tender coconuts, sweet water','Fruits',35.00,'per piece',200.00,FALSE,NULL);

-- Sample Delivery Options for Farmer 1
INSERT IGNORE INTO farmer_delivery_options (farmer_id,delivery_type,is_enabled,delivery_fee,max_distance) VALUES
(1,'FARM_PICKUP',TRUE,0.00,0),
(1,'LOCAL_DELIVERY',TRUE,50.00,25),
(1,'COURIER',FALSE,120.00,0),
(1,'COOPERATIVE_HUB',FALSE,40.00,0),
(1,'SUBSCRIPTION',FALSE,30.00,0),
(1,'COLD_CHAIN',FALSE,200.00,0);

-- Sample Delivery Options for Farmer 2
INSERT IGNORE INTO farmer_delivery_options (farmer_id,delivery_type,is_enabled,delivery_fee,max_distance) VALUES
(2,'FARM_PICKUP',TRUE,0.00,0),
(2,'LOCAL_DELIVERY',FALSE,50.00,20),
(2,'COURIER',TRUE,150.00,0),
(2,'COOPERATIVE_HUB',FALSE,40.00,0),
(2,'SUBSCRIPTION',FALSE,30.00,0),
(2,'COLD_CHAIN',FALSE,200.00,0);

-- ===================================================================
-- USEFUL QUERIES (reference)
-- ===================================================================

-- Browse all active products:
-- SELECT p.*, u.first_name, u.last_name FROM products p JOIN users u ON p.farmer_id=u.id WHERE p.is_active=1;

-- Farmer's earnings:
-- SELECT SUM(total_amount) FROM orders WHERE farmer_id=1 AND status='DELIVERED';

-- Order details with items:
-- SELECT o.*, oi.product_name, oi.quantity, oi.unit_price, oi.line_total
-- FROM orders o JOIN order_items oi ON o.id=oi.order_id WHERE o.id=1;

-- Update order status:
-- UPDATE orders SET status='CONFIRMED' WHERE id=1 AND farmer_id=1;

-- ===================================================================
-- VIEWS (optional — for reporting)
-- ===================================================================
CREATE OR REPLACE VIEW v_farmer_earnings AS
SELECT
    u.id AS farmer_id,
    CONCAT(u.first_name,' ',u.last_name) AS farmer_name,
    COUNT(o.id)                   AS total_orders,
    SUM(CASE WHEN o.status='DELIVERED' THEN o.total_amount ELSE 0 END) AS total_earned,
    SUM(CASE WHEN o.status='PENDING'   THEN 1 ELSE 0 END) AS pending_orders,
    SUM(CASE WHEN o.status='DELIVERED' THEN 1 ELSE 0 END) AS delivered_orders
FROM users u
LEFT JOIN orders o ON u.id = o.farmer_id
WHERE u.role = 'FARMER'
GROUP BY u.id, farmer_name;

CREATE OR REPLACE VIEW v_product_sales AS
SELECT
    p.id, p.name, p.category, p.price, p.unit,
    CONCAT(u.first_name,' ',u.last_name) AS farmer_name,
    SUM(oi.quantity)    AS total_sold,
    SUM(oi.line_total)  AS total_revenue,
    COUNT(oi.order_id)  AS order_count
FROM products p
JOIN users u ON p.farmer_id = u.id
LEFT JOIN order_items oi ON p.id = oi.product_id
LEFT JOIN orders o ON oi.order_id = o.id AND o.status = 'DELIVERED'
GROUP BY p.id, p.name, p.category, p.price, p.unit, farmer_name;
