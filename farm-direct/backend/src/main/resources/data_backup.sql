USE farmdirect;

ALTER TABLE products DROP FOREIGN KEY products_ibfk_1;

ALTER TABLE users MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE products MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE products MODIFY COLUMN farmer_id BIGINT NOT NULL;

ALTER TABLE products ADD CONSTRAINT products_ibfk_1 FOREIGN KEY (farmer_id) REFERENCES users(id);

INSERT IGNORE INTO users (id, name, email, password, phone, location, role, bio, created_at) VALUES
(1, 'Ravi Kumar', 'ravi@farmdirect.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '+91 98200 11111', 'Nashik, Maharashtra', 'FARMER', 'Third generation organic farmer.', NOW()),
(2, 'Meena Patel', 'meena@farmdirect.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '+91 98200 22222', 'Anand, Gujarat', 'FARMER', 'Dairy and grain farmer.', NOW()),
(3, 'Suresh Reddy', 'suresh@farmdirect.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '+91 98200 33333', 'Chittoor, Andhra Pradesh', 'FARMER', 'Mango orchard owner.', NOW()),
(4, 'Arjun Sharma', 'arjun@farmdirect.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '+91 99100 44444', 'Mumbai, Maharashtra', 'BUYER', 'Restaurant owner.', NOW()),
(5, 'Priya Iyer', 'priya@farmdirect.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '+91 99100 55555', 'Bangalore, Karnataka', 'BUYER', 'Home cook.', NOW());

INSERT IGNORE INTO products (id, name, category, description, price_per_unit, unit, quantity_available, harvest_date, image_url, farmer_id, created_at) VALUES
(1, 'Organic Tomatoes', 'Vegetables', 'Vine-ripened, pesticide-free tomatoes.', 45.00, 'kg', 120, CURDATE(), 'https://images.unsplash.com/photo-1546470427-e26264be0b0d?w=800', 1, NOW()),
(2, 'Fresh Spinach', 'Vegetables', 'Tender baby spinach harvested this morning.', 30.00, 'bunch', 80, CURDATE(), 'https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=800', 1, NOW()),
(3, 'Green Chillies', 'Vegetables', 'Spicy farm-fresh green chillies.', 60.00, 'kg', 40, CURDATE(), 'https://images.unsplash.com/photo-1583119912267-cc97c911e416?w=800', 1, NOW()),
(4, 'Stone-Milled Wheat Flour', 'Grains', 'Slow-milled whole wheat atta.', 70.00, 'kg', 200, CURDATE(), 'https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=800', 2, NOW()),
(5, 'A2 Ghee', 'Dairy', 'Pure cow ghee from grass-fed indigenous breeds.', 1200.00, 'litre', 25, CURDATE(), 'https://images.unsplash.com/photo-1628689469838-524a4a973b8e?w=800', 2, NOW()),
(6, 'Basmati Rice', 'Grains', 'Long-grain aged basmati rice.', 150.00, 'kg', 300, CURDATE(), 'https://images.unsplash.com/photo-1586201375761-83865001e31c?w=800', 2, NOW()),
(7, 'Alphonso Mangoes', 'Fruits', 'King of mangoes. Hand-picked, naturally ripened.', 400.00, 'dozen', 60, CURDATE(), 'https://images.unsplash.com/photo-1605027990121-cbae9e0642df?w=800', 3, NOW()),
(8, 'Banganapalli Mangoes', 'Fruits', 'Sweet firm-fleshed mangoes from Chittoor.', 220.00, 'dozen', 90, CURDATE(), 'https://images.unsplash.com/photo-1591073113125-e46713c829ed?w=800', 3, NOW()),
(9, 'Honey Pomelos', 'Fruits', 'Juicy sweet citrus. Great for breakfast.', 80.00, 'piece', 50, CURDATE(), 'https://images.unsplash.com/photo-1547514701-42782101795e?w=800', 3, NOW());

SELECT * FROM users;
SELECT * FROM products;