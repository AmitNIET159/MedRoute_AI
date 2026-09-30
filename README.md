# MedRoute AI

**Intelligent Healthcare Medicine & Medical Supply Logistics Platform**

> A final-year CSE major project solving real-world healthcare logistics challenges.

---

## Overview

MedRoute AI connects **Hospitals**, **Clinics**, **Pharmacies**, and **NGOs/Relief Organizations** through an intelligent logistics platform that tracks inventory, consumption patterns, shortages, expiry risks, emergency supply requests, and inter-facility stock transfers.

> **Important:** This is a healthcare *logistics* and *decision-support* system. It does not diagnose, prescribe, recommend treatment, or make clinical decisions.

---

## Tech Stack

| Component | Technology |
|---|---|
| Backend | Java 17, Spring MVC 5.3.x, JDBC, JSP/JSTL |
| Frontend | HTML5, CSS3, Bootstrap 5, Vanilla JS, Chart.js, Leaflet.js |
| Database | MySQL 8.x |
| Server | Apache Tomcat 9.0.x |
| Build | Maven 3.9.x |
| AI | Hugging Face Inference API (logistics analysis only) |
| Geocoding | OpenStreetMap Nominatim |
| Weather | Open-Meteo |
| Email | Gmail SMTP |

---

## Prerequisites

- JDK 17
- Apache Tomcat 9.0.x
- MySQL 8.x
- Maven 3.9.x
- Eclipse IDE for Enterprise Java (recommended)

---

## Quick Start

### 1. Clone & Configure

```bash
git clone <repository-url>
cd MedRoute_AI
cp .env.example .env
# Edit .env with your credentials
```

### 2. Database Setup

```sql
CREATE DATABASE medroute_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'medroute_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON medroute_ai.* TO 'medroute_user'@'localhost';
FLUSH PRIVILEGES;
```

```bash
mysql -u medroute_user -p medroute_ai < database/schema.sql
mysql -u medroute_user -p medroute_ai < database/seed.sql
```

### 3. Build & Deploy

```bash
mvn clean package -DskipTests
# Copy target/MedRouteAI.war to $CATALINA_HOME/webapps/
# Start Tomcat
```

### 4. Access

```
http://localhost:8080/MedRouteAI/
```

---

## Project Structure

```
MedRoute_AI/
├── docs/architecture/       # Architecture documentation
├── database/                # SQL schema and seed files
├── src/
│   ├── main/
│   │   ├── java/com/medroute/
│   │   │   ├── config/      # Spring configuration
│   │   │   ├── controller/  # Request handlers
│   │   │   ├── service/     # Business logic
│   │   │   ├── dao/         # Data access
│   │   │   ├── api/         # External API clients
│   │   │   ├── model/       # Domain objects
│   │   │   ├── security/    # Auth & security
│   │   │   └── util/        # Utilities
│   │   ├── resources/       # Config files
│   │   └── webapp/
│   │       ├── assets/      # CSS, JS, images
│   │       └── WEB-INF/
│   │           └── views/   # JSP templates
│   └── test/                # Unit & integration tests
├── .env.example             # Environment variable template
├── pom.xml                  # Maven build
└── README.md
```

---

## Roles

| Role | Description |
|---|---|
| **ADMIN** | Platform administration, user management, analytics |
| **HOSPITAL** | Inventory management, transfer coordination |
| **CLINIC** | Supply requests, basic inventory |
| **PHARMACY** | Stock management, order fulfillment |
| **NGO** | Relief coordination, supply distribution |

---

## Architecture Documentation

- [System Architecture](docs/architecture/architecture.md)
- [Database ER Plan](docs/architecture/database-er-plan.md)
- [Endpoint Map](docs/architecture/endpoint-map.md)
- [Security Model](docs/architecture/security-model.md)
- [Dependency Matrix](docs/architecture/dependency-matrix.md)
- [Implementation Sequence](docs/architecture/implementation-sequence.md)

---

## AI Disclaimer

All AI-generated outputs in this system are logistics insights only:

> *"AI-generated logistics insight. This is not medical advice and should not be used for clinical decision-making."*

---

## License

This project is developed as an academic final-year project.
