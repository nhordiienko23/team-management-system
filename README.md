# Team Management System

A Spring Boot backend application for managing professional sports teams, players, coaches, and users. The REST API implements role-based access control, allowing administrators to manage teams, players, coaches, users, team rosters, and player/coach transfers, while regular users have read access to sports data and can manage their own profiles. Players and coaches can be retrieved both independently and through their associated teams, including rosters and coaching staff. The application also uses OAuth2/OIDC authentication, Spring Data JPA Specifications for dynamic filtering, pagination, Liquibase migrations, centralized exception handling, AOP-based audit logging, Swagger/OpenAPI, and integration testing with Testcontainers and PostgreSQL.

## 🚀 Key Features

* **Role-Based Access Control:** Separate Admin and User capabilities. Admins can create, update, and delete teams, players, coaches, and user accounts, manage team rosters, and perform player and coach transfers. Regular users have read access to team, player, and coach information and can manage their own profiles.
* **Team & Roster Management:** Manage and view teams, team rosters, coaching staff, and team members. Players and coaches can also be retrieved independently from their associated teams.
* **RESTful API & Pagination:** REST endpoints for managing teams, players, coaches, and users with server-side pagination using `Pageable`.
* **Advanced Filtering & Search:** Dynamic filtering using Spring Data JPA Specifications, including criteria such as salary, experience, ratings, positions, and championships.
* **Security & Authentication:** OAuth2/OIDC authentication with Google, GitHub, and a custom authorization server, combined with role-based access control for Admin and User operations.
* **Audit Logging:** AOP-based logging and auditing of important application actions and method execution.
* **Database Migrations:** Version-controlled database schema changes managed with Liquibase.
* **Centralized Exception Handling:** Global exception handling with `@ControllerAdvice` for consistent API error responses.
* **API Documentation:** Interactive API documentation and endpoint testing with Springdoc OpenAPI and Swagger UI.
* **Integration Testing:** Integration tests for controllers, services, and repositories using JUnit 5, Spring Boot Test, Spring Security Test, Testcontainers, and PostgreSQL.
* **Unit Testing:** Unit tests with JUnit 5 and Mockito for business logic, DTO validation, mappers, specifications, and exception handling.
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

* **Controllers:** Handle HTTP requests, validate input, and delegate operations to services.
* **Services:** Contain business logic and transaction boundaries.
* **Repositories:** Provide data access through Spring Data JPA.
* **Specifications:** Build dynamic database queries for filtering and searching.
* **Mappers:** Convert between entities and DTOs, keeping the persistence model separated from the API layer.
* **Security:** Contains OAuth2/OIDC authentication, authorization, custom user services, and security-related handlers.
* **Audit:** Provides centralized application logging and audit tracking using Spring AOP.
  
### Configuration & Infrastructure Files

* **`docker-compose.yml`**: Defines the application and PostgreSQL services and their networking.
* **`Dockerfile`**: Defines the Docker image build process for the application.
* **`application.yml`**: Contains application configuration and environment-based settings for database and OAuth2 providers.
* **`Liquibase changelogs`**: Manage version-controlled database schema changes.

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

