# Team Management System

A production-ready Spring Boot backend application for managing professional sports teams, player rosters, and financial data. The project focuses on clean architecture, scalable REST API design, and reliable data persistence using PostgreSQL and Spring Data JPA.


## 🚀 Key Features

* RESTful API: Full CRUD operations for team management with sophisticated data filtering (salaries, rankings, experience, positions).
* RESTful API with full CRUD operations for team and player management
* Advanced server-side filtering (salary, experience, rankings, positions)
* Data persistence with PostgreSQL and Spring Data JPA
* Centralized exception handling using `@ControllerAdvice`
* Input validation for request integrity and safety
* API Documentation: Integrated Swagger UI for interactive API testing.
* Dockerized application with Docker Compose orchestration

## 🛠 Tech Stack

*Java 21
* Spring Boot 3
* Spring Data JPA
* PostgreSQL 16
* Maven
* Docker & Docker Compose
* Springdoc OpenAPI (Swagger UI)

## 🚀 Quick Start

### Prerequisites
* Docker & Docker Compose
* Java 21
* Maven

### Installation & Run

1. Clone the repository:
   ```
   git clone [https://github.com/nhordiienko23/team-management-system.git](https://github.com/nhordiienko23/team-management-system.git)
   cd team-management-system
    ```
2. Build the project:
   ```
   mvn clean package
   ```
3. Run the entire infrastructure (Database + App):
    ```
   docker-compose up --build
   ```
4. Access the interactive API documentation at:  
     ```
   http://localhost:8080/swagger-ui.html
   ```
## 🏗 Project Architecture
* docker-compose.yml: Defines the services (PostgreSQL db and the Spring Boot app) and manages networking.
* Dockerfile: Defines the multi-stage build process for the application image.
* application.yml: Centralized configuration supporting both local and Docker environments through environment variables.
