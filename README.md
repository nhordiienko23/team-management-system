# Team Management System

A production-ready Spring Boot backend application designed for managing professional sports teams, player rosters, coaches, and users. Built with clean architecture principles, it provides a scalable REST API, secure OAuth2 authentication, reliable data persistence, and comprehensive audit logging.

## 🚀 Key Features

* **Comprehensive Domain Management:** Full CRUD operations for Teams, Players, Coaches, and Users.
* **Advanced Filtering & Search:** Server-side filtering using Spring Data JPA Specifications (e.g., by salary, experience, rankings, positions).
* **Security & Authentication:** Robust OAuth2 and OIDC integration with role-based access control (differentiating between Admin and standard User operations).
* **Audit Logging:** Centralized auditing mechanism to track system actions, modifications, and user activity.
* **Database Migrations:** Version-controlled database schema management utilizing Liquibase.
* **Centralized Exception Handling:** Unified global error handling using `@ControllerAdvice` to ensure clean and predictable API responses.
* **API Documentation:** Integrated Springdoc OpenAPI (Swagger UI) for interactive API exploration and testing.
* **Dockerized Infrastructure:** Containerized application and database provisioning using Docker and Docker Compose.

## 🛠 Tech Stack

* **Core:** Java 21, Spring Boot 3
* **Data Access:** Spring Data JPA, Hibernate
* **Database:** PostgreSQL 16
* **Database Migrations:** Liquibase
* **Security:** Spring Security (OAuth2 / OIDC)
* **Build Tool:** Maven
* **Infrastructure:** Docker & Docker Compose
* **Documentation:** Springdoc OpenAPI (Swagger UI)
* **Testing:** JUnit 5, Spring Boot Test, Testcontainers (Integration Testing)

## 🏗 Project Architecture

The project follows a domain-driven package structure (`coach`, `player`, `team`, `user`, `auth`, `audit`, `security`) and implements a classic layered MVC architecture:

* **Controllers:** Handle incoming HTTP REST requests and delegate logic.
* **Services:** Encapsulate business logic and transaction boundaries.
* **Repositories:** Interface with PostgreSQL using Spring Data JPA.
* **Mappers:** Handle conversion between database Entities and DTOs to separate the domain model from the API layer.
* **Security & Configuration:** Dedicated modules for OAuth2 resource server setup, and OpenAPI configurations.
  
### Configuration & Infrastructure Files

* **`docker-compose.yml`**: Defines the services (PostgreSQL database and the Spring Boot application) and manages their networking.
* **`Dockerfile`**: Defines the multi-stage build process for creating the application's Docker image.
* **`application.yml`**: Centralized configuration file. It supports both local and Docker environments by utilizing environment variables for database connections and OAuth2 secrets.

## 🔑 OAuth2 Configuration Setup

This application is configured to act as an OAuth2 client, allowing users to log in via Google, GitHub, or a Custom Authorization Server (like [MyCustomAuthServer](https://github.com/nhordiienko23/MyCustomAuthServer)).

To run the application, you **must** configure the following environment variables. If you are running locally without Docker, you can set these in your IDE run configuration or export them in your terminal. If you are using Docker, you can pass them via a `.env` file or directly in the `docker-compose.yml`.

### Required Environment Variables

You need to obtain Client IDs and Client Secrets from the respective providers and set them as environment variables:

#### 1. Google OAuth2
* Go to the [Google Cloud Console](https://console.cloud.google.com/).
* Create a project and set up OAuth 2.0 Client IDs.
* Set the Redirect URI to: `http://localhost:8080/login/oauth2/code/google`
* **Environment Variables:**
  * `GOOGLE_CLIENTID`: Your Google Client ID
  * `GOOGLE_SECRET`: Your Google Client Secret

#### 2. GitHub OAuth2
* Go to your GitHub Developer Settings -> [OAuth Apps](https://github.com/settings/developers).
* Register a new OAuth application.
* Set the Authorization callback URL to: `http://localhost:8080/login/oauth2/code/github`
* **Environment Variables:**
  * `GITHUB_CLIENTID`: Your GitHub Client ID
  * `GITHUB_SECRET`: Your GitHub Client Secret

#### 3. Custom Authorization Server (MyCustomAuthServer)
If you are using the custom auth server, ensure it is running (by default expected on `http://localhost:9000`).
* Set the Redirect URI in the custom auth server configuration to: `http://localhost:8080/login/oauth2/code/my-custom-auth`
* **Environment Variables:**
  * `CUSTOM_AUTH_CLIENTID`: my-client-id
  * `CUSTOM_AUTH_SECRET`: my-client-secret

### application.yml Snippet Reference

The environment variables map directly to the `application.yml` configuration as follows:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENTID}
            client-secret: ${GOOGLE_SECRET}
          github:
            client-id: ${GITHUB_CLIENTID}
            client-secret: ${GITHUB_SECRET}
          my-custom-auth:
            client-name: My Custom Auth
            client-id: ${CUSTOM_AUTH_CLIENTID}
            client-secret: ${CUSTOM_AUTH_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope:
              - openid
              - profile
              - email
        provider:
          my-custom-auth:
            authorization-uri: http://localhost:9000/oauth2/authorize
            token-uri: http://localhost:9000/oauth2/token
            user-info-uri: http://localhost:9000/userinfo
            jwk-set-uri: http://localhost:9000/oauth2/jwks
            user-name-attribute: sub
```
## 🧪 Testing

The application places a strong emphasis on reliability through extensive integration testing. 
* **Integration Tests:** The `src/test/java/com/nba/` directory contains comprehensive test suites for Controllers, Services, and Repositories (e.g., `AdminCoachControllerIntegrationTest`, `CoachControllerIntegrationTest`, `UserControllerIntegrationTest`).
* These tests ensure that the Spring context loads correctly and that the application interacts seamlessly with the PostgreSQL database.

## 🚀 Quick Start

### Prerequisites
* **Java 21** (if running locally without Docker)
* **Maven** 
* **Docker & Docker Compose** (for containerized execution)

### Installation & Run

1. **Clone the repository:**
   
```bash
git clone [https://github.com/nhordiienko23/team-management-system.git](https://github.com/nhordiienko23/team-management-system.git)
cd team-management-system
```
    
2. **Build the project:**
   
```bash
mvn clean package -DskipTests
```

Note: This will spin up both the PostgreSQL container and the Spring Boot application container. Liquibase will automatically execute the changesets to build your database schema on startup.

3. **Set your environment variables. (e.g., export them in your terminal or use a .env file for Docker Compose).**

4. **Run the entire infrastructure (Database + Application):**
   
```
 docker-compose up --build
```
   
5. **Access the API Documentation:**
Open your browser and navigate to the interactive Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

