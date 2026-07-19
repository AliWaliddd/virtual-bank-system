# User Service Documentation

## 1. Purpose

The **User Service** is an independent Spring Boot microservice responsible for user registration, credential validation, and basic profile retrieval in the Virtual Bank System.

Its responsibilities are:

- Register new users.
- Prevent duplicate usernames and email addresses.
- Hash passwords before database storage.
- Validate login credentials.
- Return basic user profile information.
- Persist user data in PostgreSQL.
- Return consistent API error responses.

Implemented endpoints:

```http
POST /users/register
POST /users/login
GET  /users/{userId}/profile
```

Service port:

```text
8081
```

---

## 2. Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Programming language |
| Spring Boot | Microservice framework |
| Spring MVC | REST API implementation |
| Spring Data JPA | Repository and persistence layer |
| Hibernate | ORM implementation |
| Spring Validation | Request validation |
| Spring Security | BCrypt password hashing and security configuration |
| PostgreSQL | Persistent database |
| Docker Compose | Local PostgreSQL container |
| Maven | Build and dependency management |
| Lombok | Reduces entity boilerplate |
| Postman | Manual API testing |

Kafka dependencies are present for later request/response logging integration. Kafka publishing is deferred until the team finalizes the shared logging contract.

---

## 3. Project Structure

The User Service is a separate Spring Boot project inside the shared monorepo.

```text
virtual-bank-system/
├── user-service/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/vbank/user_service/
│       │   │       ├── UserServiceApplication.java
│       │   │       ├── config/
│       │   │       ├── controller/
│       │   │       ├── dto/
│       │   │       │   ├── request/
│       │   │       │   └── response/
│       │   │       ├── entity/
│       │   │       ├── exception/
│       │   │       ├── repository/
│       │   │       └── service/
│       │   └── resources/
│       │       └── application.yml
│       └── test/
└── infrastructure/
    └── docker-compose.yml
```

Base Java package:

```java
com.vbank.user_service
```

---

# 4. Layered Architecture

The service follows the required controller-service-repository design.

```text
HTTP Request
     |
     v
Controller Layer
     |
     v
Service Layer
     |
     v
Repository Layer
     |
     v
PostgreSQL Database
```

DTOs are used between the API and service layers so that JPA entities are never returned directly.

## 4.1 Controller Layer

Package:

```text
controller/
```

Main class:

```text
UserController
```

Responsibilities:

- Maps HTTP requests to Java methods.
- Validates request bodies using `@Valid`.
- Reads path variables and request headers.
- Calls the service layer.
- Returns the correct HTTP status and response DTO.
- Does not contain persistence logic.

Mappings:

```http
POST /users/register
POST /users/login
GET  /users/{userId}/profile
```

## 4.2 Service Layer

Package:

```text
service/
```

Main class:

```text
UserService
```

Responsibilities:

- Contains user business logic.
- Normalizes usernames and email addresses.
- Checks username and email uniqueness.
- Hashes registration passwords.
- Verifies login passwords.
- Retrieves user profiles.
- Converts entities into response DTOs.
- Throws domain-specific exceptions.

Write operations use normal transactions. Login and profile retrieval use read-only transactions.

## 4.3 Repository Layer

Package:

```text
repository/
```

Main interface:

```text
UserRepository
```

It extends:

```java
JpaRepository<User, UUID>
```

Repository methods:

```java
boolean existsByUsernameIgnoreCase(String username);
boolean existsByEmailIgnoreCase(String email);
Optional<User> findByUsernameIgnoreCase(String username);
```

Spring Data JPA generates the implementations automatically from the method names.

## 4.4 Entity Layer

Package:

```text
entity/
```

Main entity:

```text
User
```

It maps to the PostgreSQL table:

```text
users
```

The entity is not returned directly from API endpoints.

## 4.5 DTO Layer

Packages:

```text
dto/request/
dto/response/
```

Request DTOs:

- `RegisterUserRequest`
- `LoginRequest`

Response DTOs:

- `RegisterUserResponse`
- `LoginResponse`
- `UserProfileResponse`

DTOs ensure that sensitive fields such as `passwordHash` are never exposed.

## 4.6 Configuration Layer

Package:

```text
config/
```

Main configuration:

```text
SecurityConfig
```

Responsibilities:

- Provides a `PasswordEncoder` bean.
- Uses `BCryptPasswordEncoder`.
- Disables CSRF for local REST development.
- Temporarily permits requests while WSO2 integration is pending.

## 4.7 Exception Layer

Package:

```text
exception/
```

Important classes:

- `ApiError`
- `GlobalExceptionHandler`
- `UserAlreadyExistsException`
- `InvalidCredentialsException`
- `UserNotFoundException`
- `MissingAuthorizationHeaderException`

The global exception handler converts application and framework exceptions into a consistent JSON format.

---

# 5. User Entity

The `User` entity contains:

| Field | Type | Description |
|---|---|---|
| `userId` | `UUID` | Primary key generated automatically |
| `username` | `String` | Unique normalized username |
| `passwordHash` | `String` | BCrypt password hash |
| `email` | `String` | Unique normalized email address |
| `firstName` | `String` | User first name |
| `lastName` | `String` | User last name |
| `createdAt` | `Instant` | Record creation timestamp |
| `updatedAt` | `Instant` | Last update timestamp |

Important entity decisions:

- The table is named `users`.
- `username` has a database unique constraint.
- `email` has a database unique constraint.
- The raw password is never persisted.
- UUIDs are generated automatically.
- Timestamps are set through JPA lifecycle callbacks.
- The JPA no-argument constructor is protected.
- Application code uses a public constructor containing all required fields.
- Lombok `@Getter` is used.
- Lombok `@Data` is intentionally avoided because it can generate unsafe `toString()`, `equals()`, and `hashCode()` behavior involving `passwordHash`.

---

# 6. Endpoint Documentation

## 6.1 Register User

```http
POST /users/register
Content-Type: application/json
```

### Request

```json
{
  "username": "john.doe",
  "password": "securePassword123",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Processing Flow

```text
Validate request
     |
     v
Normalize username and email
     |
     v
Check username uniqueness
     |
     v
Check email uniqueness
     |
     v
Hash password using BCrypt
     |
     v
Create and save User entity
     |
     v
Return registration response
```

### Successful Response

```text
201 Created
```

```json
{
  "userId": "generated-uuid",
  "username": "john.doe",
  "message": "User registered successfully."
}
```

### Duplicate Username or Email

```text
409 Conflict
```

```json
{
  "timestamp": "2026-07-16T20:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Username or email already exists.",
  "path": "/users/register"
}
```

### Validation Rules

Username:

- Required.
- Between 3 and 50 characters.
- May contain letters, numbers, dots, underscores, and hyphens.
- Stored in lowercase.
- Checked case-insensitively.

Password:

- Required.
- Between 8 and 100 characters.
- Never trimmed by the service.
- Never returned in a response.
- Never stored as plain text.

Email:

- Required.
- Must have a valid email format.
- Maximum 255 characters.
- Stored in lowercase.
- Checked case-insensitively.

First and last names:

- Required.
- Trimmed before persistence.
- Maximum 100 characters each.

---

## 6.2 Login

```http
POST /users/login
Content-Type: application/json
```

### Request

```json
{
  "username": "john.doe",
  "password": "securePassword123"
}
```

### Processing Flow

```text
Validate presence of credentials
     |
     v
Trim and lowercase username
     |
     v
Find user by username
     |
     v
Compare raw password with BCrypt hash
     |
     +---- valid ----> return user information
     |
     +---- invalid --> return 401
```

### Successful Response

```text
200 OK
```

```json
{
  "userId": "generated-uuid",
  "username": "john.doe"
}
```

### Invalid Credentials

```text
401 Unauthorized
```

```json
{
  "timestamp": "2026-07-16T20:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password.",
  "path": "/users/login"
}
```

The same message is returned for an unknown username and an incorrect password. This avoids revealing whether a username exists.

### Login Validation Decision

Login uses only presence validation for username and password. Registration-format validation is not repeated because the account already exists and incorrect nonblank credentials should reach authentication logic and return `401`.

---

## 6.3 Retrieve User Profile

```http
GET /users/{userId}/profile
Authorization: Bearer <token>
```

### Successful Response

```text
200 OK
```

```json
{
  "userId": "generated-uuid",
  "username": "john.doe",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

The response does not expose passwords, password hashes, or internal timestamps.

### Missing User

```text
404 Not Found
```

```json
{
  "timestamp": "2026-07-16T20:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User with ID <uuid> not found.",
  "path": "/users/<uuid>/profile"
}
```

### Missing Authorization Header

```text
401 Unauthorized
```

```json
{
  "timestamp": "2026-07-16T20:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authorization header is required.",
  "path": "/users/<uuid>/profile"
}
```

### Current Authentication Limitation

At the current development stage, the backend verifies only that the `Authorization` header exists. It does not validate the token itself.

Final intended flow:

```text
Client
  |
  v
WSO2 API Gateway
  |
  | validates OAuth2 token and/or API key
  v
User Service
```

Actual token validation will be enforced during WSO2 integration.

---

# 7. Password Security

Passwords are hashed using `BCryptPasswordEncoder`.

Registration:

```java
passwordEncoder.encode(rawPassword)
```

Login:

```java
passwordEncoder.matches(rawPassword, storedHash)
```

Security properties:

- Raw passwords are never stored.
- Password hashes are never returned by API endpoints.
- Two users with the same password receive different BCrypt hashes because BCrypt uses a random salt.
- Passwords are case-sensitive.
- Unknown usernames and incorrect passwords return the same message.

Example stored hash:

```text
$2a$10$...
```

---

# 8. Database Configuration

Database:

```text
vbank_users
```

Default local connection:

```text
jdbc:postgresql://localhost:5432/vbank_users
```

Default local credentials:

```text
username: vbank
password: vbank_password
```

Configuration file:

```text
user-service/src/main/resources/application.yml
```

Example:

```yaml
server:
  port: 8081

spring:
  application:
    name: user-service

  datasource:
    url: jdbc:postgresql://localhost:5432/vbank_users
    username: vbank
    password: vbank_password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
```

`ddl-auto: update` is used for local development. A migration tool such as Flyway can be introduced later when required.

---

# 9. Docker Configuration

PostgreSQL runs through:

```text
infrastructure/docker-compose.yml
```

Start PostgreSQL:

```bash
docker compose -f infrastructure/docker-compose.yml up -d postgres
```

Check status:

```bash
docker compose -f infrastructure/docker-compose.yml ps
```

Stop without deleting data:

```bash
docker compose -f infrastructure/docker-compose.yml stop postgres
```

Start again:

```bash
docker compose -f infrastructure/docker-compose.yml start postgres
```

Remove containers while preserving the named volume:

```bash
docker compose -f infrastructure/docker-compose.yml down
```

Delete containers and local database data only when intentional:

```bash
docker compose -f infrastructure/docker-compose.yml down -v
```

The named Docker volume preserves users after application and container restarts.

---

# 10. Error Handling

All API errors follow this structure:

```json
{
  "timestamp": "2026-07-16T20:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Description of the error.",
  "path": "/requested/path"
}
```

Handled conditions:

| Condition | Status |
|---|---:|
| Invalid request fields | `400` |
| Malformed or missing JSON | `400` |
| Invalid UUID path value | `400` |
| Invalid login credentials | `401` |
| Missing authorization header | `401` |
| Missing user | `404` |
| Unknown endpoint | `404` |
| Unsupported HTTP method | `405` |
| Duplicate username or email | `409` |
| Unsupported content type | `415` |
| Unexpected internal error | `500` |

Framework exception handling includes:

- `MethodArgumentNotValidException`
- `MethodArgumentTypeMismatchException`
- `HttpMessageNotReadableException`
- `HttpRequestMethodNotSupportedException`
- `HttpMediaTypeNotSupportedException`
- `NoResourceFoundException`
- `NoHandlerFoundException`

The generic `Exception` handler remains the final fallback.

---

# 11. Manual Testing Completed

The service was tested manually using Postman.

## Registration

Verified:

- Successful registration.
- UUID generation.
- Username and email normalization.
- Duplicate username and email handling.
- Case-insensitive duplicate checks.
- Required-field validation.
- Username format validation.
- Password length validation.
- Email validation.
- Password exclusion from responses.
- BCrypt storage in PostgreSQL.
- Different hashes for identical raw passwords.

## Login

Verified:

- Successful login.
- Case-insensitive username lookup.
- Username trimming.
- Incorrect password handling.
- Password case sensitivity.
- Unknown username handling.
- Blank and missing credentials.
- Email cannot be used instead of username.
- SQL-injection-like input is treated as ordinary text.

## Profile

Verified:

- Successful profile retrieval.
- Missing authorization header.
- Missing user.
- Invalid UUID.
- Password hash is never returned.
- Temporary arbitrary Bearer-token behavior is documented.

## HTTP Robustness

Verified:

- Unsupported method returns `405`.
- Unsupported content type returns `415`.
- Malformed JSON returns `400`.
- Missing JSON body with `application/json` returns `400`.
- Missing content type returns `415`.
- Unknown endpoint returns `404`.

## Persistence

Verified:

- Data is stored in PostgreSQL.
- Passwords are hashed.
- Unique constraints exist.
- Failed requests do not create rows.
- Data survives Spring Boot restart.
- Data survives PostgreSQL container restart.

---

# 12. Running the Service

From the repository root, start PostgreSQL:

```bash
docker compose -f infrastructure/docker-compose.yml up -d postgres
```

Enter the service directory:

```bash
cd user-service
```

Build and test:

```bash
./mvnw clean test
```

Run:

```bash
./mvnw spring-boot:run
```

Expected startup output includes:

```text
Tomcat started on port 8081
Started UserServiceApplication
```

Base URL:

```text
http://localhost:8081
```

---

# 13. Completion Status

Completed:

```text
[x] Spring Boot project created
[x] PostgreSQL connected
[x] Docker PostgreSQL configured
[x] User entity implemented
[x] Registration implemented
[x] Login implemented
[x] Profile retrieval implemented
[x] BCrypt password hashing implemented
[x] Duplicate username/email protection implemented
[x] Validation implemented
[x] Global exception handling implemented
[x] Manual Postman testing completed
[x] Password storage verified
[x] Database persistence verified
[x] User Service merged into main
```

Pending integration work:

```text
[ ] Kafka request/response logging
[ ] WSO2 OAuth2 validation
[ ] APP-NAME propagation and logging
[ ] X-Correlation-ID propagation
[ ] Expanded automated unit tests
[ ] Expanded automated integration tests
```

These pending items belong to the integration stage and do not block Account Service development.

---

# 14. Design Decisions Summary

| Decision | Current Choice |
|---|---|
| Service port | `8081` |
| Database | PostgreSQL |
| Database name | `vbank_users` |
| Primary key | UUID |
| Password storage | BCrypt hash |
| Username comparison | Case-insensitive |
| Email comparison | Case-insensitive |
| Username storage | Lowercase |
| Email storage | Lowercase |
| API entity exposure | Entities are not returned |
| Development authentication | Authorization-header presence |
| Final authentication | WSO2 OAuth2/API-key validation |
| Error format | Shared `ApiError` response |
| Timestamp type | `Instant` |
| Build tool | Maven |
| Java version | Java 21 |
| Kafka integration | Deferred to shared logging phase |

---

# 15. Next Development Step

Member 1 can now proceed to the Account Service.

Recommended order:

```text
1. Create the Account Service Spring Boot project
2. Configure port 8082
3. Create the vbank_accounts database
4. Create AccountType and AccountStatus enums
5. Create the Account entity
6. Implement POST /accounts
7. Implement GET /accounts/{accountId}
8. Implement GET /users/{userId}/accounts
9. Implement PUT /accounts/transfer
10. Add transactional and concurrency-safe balance updates
11. Add lastActivityAt
12. Implement the hourly inactivity scheduler
13. Test all Account Service behavior
```
