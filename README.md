# Team Management System

A robust Spring Boot backend application designed for managing professional sports rosters and financial data. The project emphasizes clean architecture, API reliability, and effective data management using Object-Oriented Programming (OOP) principles, now fully integrated with PostgreSQL and Docker.

## 🚀 Key Features

* **RESTful API**: Full CRUD operations for team management with sophisticated server-side filtering (salary, ratings, experience, positions).
* **Data Integrity**: Strict validation via `Jakarta Validation` and `GlobalExceptionHandler` for standardized, secure error responses.
* **Test-Driven Foundation**: Comprehensive test coverage including unit tests for business logic, integration tests for the controller layer, DTO validation, and AOP-based audit logging.
* **Resilience**: Caching support via Spring Cache and database persistence with PostgreSQL.
* **DevOps Ready**: Fully dockerized environment with multi-stage Docker builds and Docker Compose orchestration.
* **API Documentation**: Interactive documentation available via Swagger UI.

## 🛠 Tech Stack

| Component | Technology |
| :--- | :--- |
| **Framework** | Spring Boot 3, Spring Data JPA |
| **Language** | Java 21 |
| **Database** | PostgreSQL 16 |
| **Testing** | JUnit 5, Mockito, MockMvc |
| **DevOps** | Docker, Docker Compose |
| **Documentation** | Springdoc OpenAPI (Swagger UI) |

## 🧪 Testing Coverage

The project is thoroughly tested to ensure stability and reliability:
* **Unit Tests**: Business logic in `Player`, `Coach`, and `Staff` models, including custom bonus calculation formulas.
* **Controller Tests**: Integration tests for all API endpoints using `MockMvc` to ensure reliable request/response handling.
* **Validation Tests**: Verification of `StaffDto` constraints and error handling in `GlobalExceptionHandler`.
* **Aspect Tests**: Verification of automated audit logging via `LoggingAspect`.

## 🚀 Quick Start

### Prerequisites
* Docker & Docker Compose
* Java 21
* Maven

### Installation & Run

1. Clone the repository:
   ```
   git clone [ https://github.com/nhordiienko23/team-management-system.git](https://github.com/nhordiienko23/team-management-system.git)
   cd team-management-system
    ```
2. Run the entire infrastructure (Database + App):
    ```
   docker-compose up --build
   ```
3. Access the interactive API documentation at:  
     ```
   http://localhost:8080/swagger-ui.html
   ```
## 🏗 Project Architecture
* src/main/java: Core business logic, controllers, services, and models.
* src/test/java: Full suite of unit and integration tests covering the entire application lifecycle.
* docker-compose.yml: Defines the services (PostgreSQL db and the Spring Boot app) and manages networking.
* Dockerfile: Defines the multi-stage build process for the application image.
* application.yml: Centralized configuration supporting both local and Docker environments through environment variables.
