# FarmDirect — Farmer to Buyer Marketplace

A full-stack platform that connects farmers directly with buyers — **no middlemen, no commissions, fair prices for everyone**.

## Tech Stack

- **Frontend:** HTML5, CSS3, Vanilla JavaScript
- **Backend:** Java 17, Spring Boot 3.2 (REST API, Spring Security, JWT)
- **Database:** MySQL 8
- **Build:** Maven

## Features

### For Farmers
- Register / login as a farmer
- List crops and produce with photos, price, quantity, and harvest details
- Manage product inventory (add / edit / delete)
- View incoming orders from buyers
- Update order status (Pending → Confirmed → Shipped → Delivered)
- Dashboard with sales analytics

### For Buyers
- Register / login as a buyer
- Browse fresh produce from local farmers
- Search and filter by category, price, location
- View farmer profile and rating
- Add to cart and place orders
- Track order status in real time
- Order history

### Platform
- JWT-based authentication
- Role-based access control (FARMER / BUYER)
- Direct messaging between buyer and farmer (contact info shared on order)
- Responsive, modern UI with attractive design

---

## Project Structure

```
farm-direct/
├── backend/          → Spring Boot REST API
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/farmdirect/    → Java source
│       └── resources/
│           ├── application.properties
│           └── data.sql
└── frontend/         → HTML / CSS / JS client
    ├── index.html
    ├── login.html
    ├── register.html
    ├── products.html
    ├── product-detail.html
    ├── farmer-dashboard.html
    ├── buyer-dashboard.html
    ├── css/style.css
    └── js/api.js
```

---

## Setup & Running

### 1. Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+
- Any modern browser

### 2. Database setup

Open MySQL and run:

```sql
CREATE DATABASE farmdirect;
CREATE USER 'farmuser'@'localhost' IDENTIFIED BY 'farmpass';
GRANT ALL PRIVILEGES ON farmdirect.* TO 'farmuser'@'localhost';
FLUSH PRIVILEGES;
```

(Tables are created automatically by Hibernate on first run; sample seed data is loaded from `data.sql`.)

### 3. Configure the backend

Edit `backend/src/main/resources/application.properties` if your MySQL credentials are different:

```
spring.datasource.url=jdbc:mysql://localhost:3306/farmdirect
spring.datasource.username=farmuser
spring.datasource.password=farmpass
```

### 4. Run the backend

```bash
cd backend
mvn spring-boot:run
```

The API will start at `http://localhost:8080`.

### 5. Open the frontend

Just open `frontend/index.html` in your browser, or serve it with any static server:

```bash
cd frontend
python -m http.server 5500
# then visit http://localhost:5500
```

### 6. Test accounts (auto-seeded)

| Role   | Email                  | Password   |
| ------ | ---------------------- | ---------- |
| Farmer | ravi@farmdirect.com    | password   |
| Farmer | meena@farmdirect.com   | password   |
| Buyer  | arjun@farmdirect.com   | password   |
| Buyer  | priya@farmdirect.com   | password   |

---

## API Endpoints (Quick Reference)

| Method | Endpoint                   | Description                          | Auth        |
| ------ | -------------------------- | ------------------------------------ | ----------- |
| POST   | /api/auth/register         | Register a farmer or buyer           | Public      |
| POST   | /api/auth/login            | Login, returns JWT                   | Public      |
| GET    | /api/products              | List all products (with filters)     | Public      |
| GET    | /api/products/{id}         | Get a single product                 | Public      |
| POST   | /api/products              | Create a product                     | FARMER      |
| PUT    | /api/products/{id}         | Update a product                     | FARMER      |
| DELETE | /api/products/{id}         | Delete a product                     | FARMER      |
| GET    | /api/products/mine         | List my own products                 | FARMER      |
| POST   | /api/orders                | Place an order                       | BUYER       |
| GET    | /api/orders/buyer          | My orders as a buyer                 | BUYER       |
| GET    | /api/orders/farmer         | Orders for my products               | FARMER      |
| PUT    | /api/orders/{id}/status    | Update order status                  | FARMER      |
| GET    | /api/users/me              | Current user profile                 | Authed      |

---

## Why FarmDirect is Unique

- **Zero commission** — farmers keep 100% of the sale
- **Direct contact** — buyers can call/message farmers after an order
- **Transparent pricing** — farmers set their own prices, no opaque markup
- **Local first** — filter by region to support nearby growers
- **Harvest dates visible** — buyers know exactly how fresh the produce is
- **Farmer profiles** — every farmer has a story, photo, and rating

Built with care for farming communities.
