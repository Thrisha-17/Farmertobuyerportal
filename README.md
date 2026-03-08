# FarmConnect – Spring Boot Backend

## Quick Start

### 1. Prerequisites
- Java 21+
- Maven 3.8+
- MySQL 8.x running on port 3306

### 2. Configure DB
Edit `src/main/resources/application.properties`:
```
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### 3. Run
```bash
mvn spring-boot:run
```
Server starts at: **http://localhost:8080**

---

## Project Structure
```
src/main/java/com/farmconnect/
├── FarmConnectApplication.java
├── config/       SecurityConfig.java
├── controller/   Auth, Product, Order, Delivery, Rating
├── dto/          Request/Response objects
├── exception/    GlobalExceptionHandler + custom exceptions
├── model/        JPA entities (8 tables)
├── repository/   Spring Data JPA repositories
├── security/     JWT filter + utility
└── service/      Business logic (5 services)
```

---

## API Endpoints

### Auth (Public)
```
POST /api/auth/register   → Register farmer or buyer
POST /api/auth/login      → Login, returns JWT
```

### Products
```
GET  /api/products/public              → Browse all (no auth needed)
GET  /api/products/public?category=X  → Filter by category
GET  /api/products/public?search=X    → Search
GET  /api/products/public?organic=true→ Organic only
POST /api/farmer/products/{farmerId}  → Add product (FARMER only)
PUT  /api/farmer/products/{fid}/{pid} → Update product + price
DELETE /api/farmer/products/{fid}/{pid}→ Remove product
```

### Orders
```
POST   /api/buyer/orders/{buyerId}                  → Place order
GET    /api/buyer/orders/{buyerId}                  → My orders
DELETE /api/buyer/orders/{buyerId}/{orderId}/cancel → Cancel order
GET    /api/farmer/orders/{farmerId}                → Incoming orders
PATCH  /api/farmer/orders/{fid}/{oid}/status?status=CONFIRMED → Update status
GET    /api/farmer/orders/{farmerId}/earnings       → Total earnings
```

### Delivery Options
```
GET /api/farmer/delivery/{farmerId}         → All 6 options
PUT /api/farmer/delivery/{farmerId}         → Toggle/update option
GET /api/farmer/delivery/{farmerId}/enabled → Enabled options only
```

### Ratings
```
POST /api/ratings/buyer/{buyerId}    → Rate a delivered order
GET  /api/ratings/farmer/{farmerId}  → View farmer ratings
```

---

## Auth Header
All protected endpoints require:
```
Authorization: Bearer <jwt_token>
```
