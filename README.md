# Stock Ledger

An append-only stock ledger for tracking items across warehouses. Stock
numbers are never stored and overwritten - they are always calculated from
the full history of movements. See `DESIGN.md` for the reasoning behind the
key decisions.

## What it is

- Java 17, Spring Boot 3.3, Spring Data JPA / Hibernate, MySQL.
- No login/auth - this explicitly doesn't require user accounts.
  `recordedBy` is just a free-text field passed in with each request.

## Requirements

- Java 17+
- Maven 3.8+
- MySQL 8.x running locally

## Database setup

The application creates the database automatically on first connection
(`createDatabaseIfNotExist=true`), and Hibernate creates/updates the tables
on startup (`ddl-auto=update`). You only need MySQL itself running and
reachable - no manual schema setup needed.

Default connection (override with environment variables if yours differs):

| Setting | Default | Env var to override |
|---|---|---|
| URL | `jdbc:mysql://localhost:3306/stock_ledger` | `DB_URL` |
| Username | `root` | `DB_USERNAME` |
| Password | `root` | `DB_PASSWORD` |

Example, if your MySQL root password is different:

```bash
export DB_PASSWORD=your_actual_password
```

## How to run

```bash
mvn clean install
mvn spring-boot:run
```

The app starts on `http://localhost:8080` by default (override with `SERVER_PORT`).

## How to run the tests

Tests run against a **real MySQL database** (`stock_ledger_test`), not an
in-memory one. Make sure MySQL is running,
then:

```bash
mvn test
```

The test schema is dropped and recreated on every run (`ddl-auto=create-drop`
in `src/test/resources/application-test.properties`), so tests always start
from a clean slate. Each test also generates unique item/warehouse codes
(random suffixes) so tests don't collide with each other's data.

## Quick start - seed some demo data

Once the app is running, call:

```bash
curl -X POST http://localhost:8080/seed
```

This creates 3 items (`PEN-BLUE`, `NOTE-A4`, `OIL-SUN`) and 2 warehouses
(`WH-NORTH`, `WH-SOUTH`) if they don't already exist, so you can start
recording movements immediately. Safe to call more than once.

## Endpoints

### Items

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/items` | `{"code":"PEN-BLUE","name":"Blue Pen","unit":"pieces"}` | create |
| GET | `/get-items` | - | list all |
| PATCH | `/items/{code}/disable` | - | disable |
| PATCH | `/items/{code}/rename` | `{"name":"Blue Gel Pen"}` | rename (history unaffected |

### Warehouses

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/warehouses` | `{"code":"WH-NORTH","name":"North Warehouse"}` | create |
| GET | `/warehouses` | - | list all |
| PATCH | `/warehouses/{code}/disable` | - | disable |

### Movements

**Record an IN:**
```bash
curl -X POST http://localhost:8080/movements \
  -H "Content-Type: application/json" \
  -d '{
    "kind": "IN",
    "itemCode": "PEN-BLUE",
    "warehouseCode": "WH-NORTH",
    "quantity": 100,
    "reason": "purchase order 4471",
    "occurredAt": "2026-08-10T09:00:00",
    "recordedBy": "purnesh"
  }'
```

**Record an OUT:**
```bash
curl -X POST http://localhost:8080/movements \
  -H "Content-Type: application/json" \
  -d '{
    "kind": "OUT",
    "itemCode": "PEN-BLUE",
    "warehouseCode": "WH-NORTH",
    "quantity": 15,
    "reason": "sales order 991",
    "occurredAt": "2026-08-11T10:00:00",
    "recordedBy": "purnesh"
  }'
```

**Record a TRANSFER:**
```bash
curl -X POST http://localhost:8080/movements \
  -H "Content-Type: application/json" \
  -d '{
    "kind": "TRANSFER",
    "itemCode": "PEN-BLUE",
    "fromWarehouseCode": "WH-NORTH",
    "toWarehouseCode": "WH-SOUTH",
    "quantity": 20,
    "reason": "rebalancing stock",
    "occurredAt": "2026-08-11T12:00:00",
    "recordedBy": "purnesh"
  }'
```

**Cancel a movement:**
```bash
curl -X POST http://localhost:8080/movements/1/cancel \
  -H "Content-Type: application/json" \
  -d '{"recordedBy": "purnesh", "reason": "wrong quantity entered"}'
```

**Movement history (paginated, newest first):**
```bash
curl "http://localhost:8080/movements?itemCode=PEN-BLUE&page=0&size=20"
```

### Stock

**Current stock, one warehouse:**
```bash
curl "http://localhost:8080/stock/current?itemCode=PEN-BLUE&warehouseCode=WH-NORTH"
```

**Current stock, all warehouses:**
```bash
curl "http://localhost:8080/stock/current?itemCode=PEN-BLUE"
```

**Stock on a past date/time, one warehouse:**
```bash
curl "http://localhost:8080/stock/at-date?itemCode=PEN-BLUE&warehouseCode=WH-NORTH&at=2026-03-03T00:00:00"
```

**Stock on a past date/time, all warehouses:**
```bash
curl "http://localhost:8080/stock/at-date?itemCode=PEN-BLUE&at=2026-03-03T00:00:00"
```

## Error format

All errors return a consistent JSON body:

```json
{
  "timestamp": "2026-08-12T10:15:30",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Insufficient stock: requested 10 but only 5 available"
}
```

- `400` - malformed request (validation failure, missing required field)
- `404` - item/warehouse/movement not found
- `422` - well-formed request that breaks a business rule (insufficient stock,
  already-cancelled movement, disabled item, duplicate code)
  
Author

Dubbala Purnesh Reddy

Java Backend Developer | Spring Boot | Hibernate | MySQL | Spring MVC | Maven

📱 LinkedIn: https://www.linkedin.com/in/purnesh-reddy-d-642578312

🐙 GitHub: https://github.com/PurneshReddy-D 📧 Email: dubbalapurnesh878681@gmail.com
