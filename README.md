# Virtual Bank System

A one-month internship project that implements a simplified virtual banking platform using Spring Boot microservices, a Backend for Frontend (BFF), Apache Kafka, PostgreSQL, Docker Compose, and WSO2 API Manager.

## Project Overview

The system is divided into independent Spring Boot services:

```text
Client / Postman
       |
       v
WSO2 API Gateway
       |
       +--------------------+
       |                    |
       v                    v
  BFF Service        Direct API routing
       |                    |
       +---------+----------+
                 |
      +----------+-----------+
      |          |           |
      v          v           v
 User Service  Account    Transaction
               Service      Service

All application services publish logs to Kafka
                 |
                 v
          Logging Service
                 |
                 v
          Logging Database
```

Each microservice is an independent Maven/Spring Boot project with its own source code, configuration, tests, port, and—where required—database.

---

## Repository Structure

```text
virtual-bank-system/
├── user-service/
├── account-service/
├── transaction-service/
├── bff-service/
├── logging-service/
├── infrastructure/
│   ├── docker-compose.yml
│   ├── postgres/
│   └── kafka/
├── wso2/
├── postman/
├── docs/
│   ├── api-contracts/
│   ├── diagrams/
│   └── architecture-decisions.md
├── .env.example
├── .gitignore
└── README.md
```

---

# Team Division

The work is divided by **service ownership**. Each member is responsible for the full implementation of the services assigned to them, including controllers, DTOs, entities, repositories, service logic, configuration, exception handling, tests, documentation, and integration.

## Member 1 — User, Account, and BFF

### User Service

Responsible for:

- `POST /users/register`
- `POST /users/login`
- `GET /users/{userId}/profile`
- User entity and database schema
- Unique username and email validation
- Password hashing using BCrypt
- Request validation
- Authentication-related error handling
- Preventing passwords from appearing in responses or logs
- Kafka request/response logging
- Unit and integration tests

### Account Service

Responsible for:

- `POST /accounts`
- `GET /accounts/{accountId}`
- `GET /users/{userId}/accounts`
- `PUT /accounts/transfer`
- Account entity and database schema
- Account number generation
- Account types:
    - `SAVINGS`
    - `CHECKING`
    - `SYSTEM`
- Account statuses:
    - `ACTIVE`
    - `INACTIVE`
- Positive balance and transfer amount validation
- Sufficient-funds validation
- Preventing transfers between the same account
- Atomic debit and credit operations using `@Transactional`
- Concurrency protection for account balances
- Updating account activity timestamps
- Kafka request/response logging
- Unit and integration tests

### Account Inactivity Scheduled Job

Responsible for:

- Running every hour
- Finding accounts where:
    - status is `ACTIVE`
    - `lastActivityAt` is older than 24 hours
- Changing their status to `INACTIVE`
- Defining the behavior for newly created accounts
- Testing the scheduled logic

### BFF Service

Responsible for:

- `GET /bff/dashboard/{userId}`
- Calling User Service for profile details
- Calling Account Service for user accounts
- Calling Transaction Service for each account's transactions
- Using `WebClient`
- Retrieving account transactions asynchronously
- Combining responses into one dashboard response
- Forwarding important headers:
    - `Authorization`
    - `APP-NAME`
    - `X-Correlation-ID`
- Handling downstream-service failures
- Kafka request/response logging
- Unit and integration tests using mocked services

---

## Member 2 — Transactions, Kafka, Logging, and WSO2

### Transaction Service

Responsible for:

- `POST /transactions/transfer/initiation`
- `POST /transactions/transfer/execution`
- `GET /accounts/{accountId}/transactions`
- Transaction entity and database schema
- Transaction statuses:
    - `INITIATED`
    - `SUCCESS`
    - `FAILED`
- Calling Account Service using `WebClient`
- Creating transaction records before execution
- Updating transaction status after execution
- Saving failure reasons
- Preventing duplicate transaction execution
- Making execution idempotent
- Returning sent and received transaction history
- Kafka request/response logging
- Unit and integration tests

### Kafka Infrastructure

Responsible for:

- Adding Kafka to Docker Compose
- Defining the Kafka topic
- Configuring producers and consumers
- Defining the shared log-message format
- Documenting Kafka environment variables
- Testing message production and consumption
- Providing a standard producer pattern for all services

Each member integrates the Kafka producer into the services they own.

### Logging Service

Responsible for:

- Consuming request and response logs from Kafka
- Parsing Kafka messages
- Saving logs in its database
- Handling malformed messages
- Configuring consumer groups
- Ensuring logs do not contain:
    - passwords
    - access tokens
    - API keys
    - authorization headers
- Unit and integration tests

### WSO2 API Manager

Responsible for:

- Creating the Register API
- Creating the Login API
- Creating the Dashboard API
- Creating the Transactions API
- Creating the `vbank` API product
- Configuring backend routing
- Configuring OAuth2
- Configuring API-key security
- Configuring throttling
- Creating two applications:
    - `vbank portal`
    - `vbank mobile`
- Injecting the `APP-NAME` header:
    - `PORTAL`
    - `MOBILE`
- Exporting or documenting WSO2 configuration
- Maintaining the final gateway-based Postman collection

---

# Service Ports

| Component | Port |
|---|---:|
| User Service | `8081` |
| Account Service | `8082` |
| Transaction Service | `8083` |
| BFF Service | `8084` |
| Logging Service | `8085` |
| PostgreSQL | `5432` |
| Kafka | `9092` |
| WSO2 API Manager | To be confirmed from WSO2 configuration |

Internal service URLs must be configured using environment variables rather than hardcoded in Java classes.

Example:

```env
USER_SERVICE_URL=http://user-service:8081
ACCOUNT_SERVICE_URL=http://account-service:8082
TRANSACTION_SERVICE_URL=http://transaction-service:8083
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

For local development outside Docker, the values may use `localhost`.

---

# Database Ownership

Recommended design:

| Service | Database |
|---|---|
| User Service | `vbank_users` |
| Account Service | `vbank_accounts` |
| Transaction Service | `vbank_transactions` |
| Logging Service | `vbank_logs` |
| BFF Service | No database |

One PostgreSQL server may host all four logical databases, but each service must access only its own database.

Services must communicate through REST APIs or Kafka. A service must not query another service's tables directly.

---

# Shared Technology Decisions

Recommended baseline:

| Item | Decision |
|---|---|
| Java | Java 11 |
| Spring Boot | Java-11-compatible Spring Boot version |
| Build tool | Maven |
| Database | PostgreSQL |
| Internal HTTP client | Spring `WebClient` |
| Messaging | Apache Kafka |
| Containers | Docker Compose |
| API Gateway | WSO2 API Manager |
| API testing | Postman |
| Unit testing | JUnit and Mockito |

Both members must use the same Java and Maven versions.

---

# Decisions That Must Be Confirmed Together

The following decisions must be discussed and recorded before implementation. Final decisions should also be copied into `docs/architecture-decisions.md`.

## 1. Mandatory Project Scope

Confirm whether the daily interest feature is required for a two-member team.

Current proposed scope:

### Mandatory

- User Service
- Account Service
- Transaction Service
- BFF Service
- Logging Service
- Kafka
- WSO2 API Manager
- Account inactivity scheduler

### Pending Supervisor Confirmation

- Daily savings-interest scheduler
- Persisted `SYSTEM` account
- Automatic interest payouts

Decision:

```text
[ ] Daily interest is required
[ ] Daily interest is not required
[ ] Waiting for supervisor confirmation
```

---

## 2. WSO2 Routing

Recommended routing:

| Public Route | Backend |
|---|---|
| `/register` | User Service `/users/register` |
| `/login` | User Service `/users/login` |
| `/dashboard/{userId}` | BFF `/bff/dashboard/{userId}` |
| `/initiation` | Transaction Service `/transactions/transfer/initiation` |
| `/execution` | Transaction Service `/transactions/transfer/execution` |

Decision:

```text
[ ] Use the routing above
[ ] Route every external request through the BFF
[ ] Other: ______________________________
```

---

## 3. Authentication Responsibility

The project requires WSO2 OAuth2 and API-key security, but it does not clearly state whether User Service must generate JWTs.

Confirm:

```text
[ ] WSO2 manages external access tokens; User Service only validates credentials
[ ] User Service also generates a JWT
[ ] OAuth2 and API key are both required for every request
[ ] Clients may use either OAuth2 or API key
[ ] Waiting for supervisor confirmation
```

---

## 4. Account Transfer Design

Recommended decision:

- Transaction Service calls one Account Service endpoint.
- Account Service performs debit and credit atomically.
- Separate debit and credit API calls will not be used.

Endpoint:

```http
PUT /accounts/transfer
```

Request:

```json
{
  "fromAccountId": "uuid",
  "toAccountId": "uuid",
  "amount": 100.00
}
```

Decision:

```text
[ ] Use one atomic transfer endpoint
[ ] Use separate debit and credit endpoints
```

---

## 5. Transaction Execution and Idempotency

Recommended behavior:

1. Initiation creates an `INITIATED` transaction.
2. Execution calls Account Service.
3. Success changes status to `SUCCESS`.
4. Failure changes status to `FAILED`.
5. A `SUCCESS` transaction cannot be executed again.
6. Repeated execution returns `409 Conflict`.

Decision:

```text
[ ] Reject repeated execution with 409
[ ] Allow controlled retries for FAILED transactions
[ ] Other: ______________________________
```

---

## 6. Account Activity Rule

Recommended design:

- Add `lastActivityAt` to the Account entity.
- Set it to account creation time when the account is created.
- Update it when an account sends or receives money.
- Run the inactivity job every hour.
- Mark accounts inactive after 24 hours without activity.

Confirm whether inactive accounts may receive money:

```text
[ ] Inactive accounts cannot send or receive
[ ] Inactive accounts may receive but cannot send
[ ] Other: ______________________________
```

---

## 7. Dashboard Transaction Limit

The project says "recent transactions" but does not define how many.

Decision:

```text
[ ] Return the latest 10 transactions per account
[ ] Return all transactions
[ ] Use pagination
[ ] Other limit: __________
```

Recommended initial choice: latest 10 transactions per account.

---

## 8. Dashboard Failure Behavior

Recommended behavior:

- Missing user returns `404`.
- User or Account Service failure causes the dashboard request to fail.
- Transaction Service failure causes the dashboard request to return `502 Bad Gateway`.

Decision:

```text
[ ] Fail the complete dashboard when a downstream service fails
[ ] Return partial data with warnings
```

---

## 9. Transaction History Format

Recommended rules:

- Store all transfer amounts as positive `BigDecimal` values.
- Use `fromAccountId` and `toAccountId` to determine direction.
- Use only the `status` field.
- Do not use inconsistent fields such as `deliveryStatus`.
- Return transactions where the account is either sender or receiver.

Decision:

```text
[ ] Use this transaction-history format
[ ] Other: ______________________________
```

---

## 10. Kafka Topic and Log Format

Recommended topic:

```text
vbank.logs
```

Minimum required message:

```json
{
  "message": "{}",
  "messageType": "REQUEST",
  "dateTime": "2026-07-16T12:30:00Z"
}
```

Recommended extended message:

```json
{
  "message": "{}",
  "messageType": "REQUEST",
  "dateTime": "2026-07-16T12:30:00Z",
  "serviceName": "account-service",
  "httpMethod": "PUT",
  "path": "/accounts/transfer",
  "statusCode": null,
  "correlationId": "uuid",
  "appName": "PORTAL"
}
```

Decision:

```text
[ ] Use only the minimum fields
[ ] Use the extended fields
[ ] Other: ______________________________
```

---

## 11. Correlation ID

Recommended header:

```http
X-Correlation-ID
```

Proposed behavior:

- WSO2 or BFF creates one when it is missing.
- Services forward it to downstream services.
- Kafka logs include it.
- Error responses include it.

Decision:

```text
[ ] Use X-Correlation-ID
[ ] Do not use a correlation ID
[ ] Other header: ________________________
```

---

## 12. `APP-NAME` Behavior

Allowed values:

```text
PORTAL
MOBILE
```

Recommended rules:

- WSO2 determines the calling application.
- WSO2 replaces any client-provided `APP-NAME`.
- BFF forwards it to downstream services.
- Backend services include it in logs.
- Backends do not trust arbitrary client-supplied values.

Decision for invalid or missing values:

```text
[ ] Return 400 Bad Request
[ ] Return 403 Forbidden
[ ] Allow missing APP-NAME for internal calls
[ ] Other: ______________________________
```

---

## 13. Common Error Response

Recommended format:

```json
{
  "timestamp": "2026-07-16T12:45:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Account not found.",
  "path": "/accounts/123",
  "correlationId": "uuid"
}
```

Recommended status codes:

| Status | Meaning |
|---:|---|
| `400` | Invalid input or business-rule violation |
| `401` | Unauthenticated |
| `403` | Authenticated but unauthorized |
| `404` | Resource not found |
| `409` | Duplicate or conflicting operation |
| `500` | Unexpected internal error |
| `502` | Downstream service failure |
| `503` | Service unavailable |

Decision:

```text
[ ] Use this error format in every service
[ ] Other: ______________________________
```

---

## 14. Shared Data Types

Recommended:

```text
userId        UUID
accountId     UUID
transactionId UUID
accountNumber String
money         BigDecimal
timestamps    Instant or OffsetDateTime
```

Decision:

```text
[ ] Use Instant
[ ] Use OffsetDateTime
```

Never use `double` or `float` for money.

---

## 15. API Contracts

Before implementation, create:

```text
docs/api-contracts/
├── user-service.md
├── account-service.md
├── transaction-service.md
├── bff-service.md
└── logging-message.md
```

Every endpoint contract must define:

- HTTP method
- path
- required headers
- request fields
- response fields
- validation rules
- success status
- possible error statuses
- example JSON

Decision:

```text
[ ] Contracts will be written before implementation
[ ] Contracts will be generated using OpenAPI
[ ] Both Markdown and OpenAPI will be used
```

---

# Git and GitHub Workflow

## Main Rule

Do not push unfinished work directly to `main`.

Use one branch per feature or fix.

Examples:

```text
feature/user-registration
feature/account-creation
feature/account-transfer
feature/transaction-initiation
feature/transaction-execution
feature/bff-dashboard
feature/kafka-logging
feature/wso2-configuration
fix/duplicate-transaction-execution
```

## Starting a Task

```bash
git checkout main
git pull origin main
git checkout -b feature/task-name
```

## Saving and Pushing Work

```bash
git add .
git commit -m "Implement user registration endpoint"
git push -u origin feature/task-name
```

Then open a pull request from the feature branch into `main`.

## After a Pull Request Is Merged

```bash
git checkout main
git pull origin main
git branch -d feature/task-name
```

## Pull Request Review Checklist

Before merging, the other member should verify:

- The code follows the agreed API contract.
- Validation is present.
- Errors use the common response format.
- Sensitive data is not logged.
- Tests are included.
- No secrets are committed.
- Existing services still build.
- Shared configuration changes are explained.

---

# Shared-File Ownership

To reduce merge conflicts:

| Shared File or Folder | Primary Owner |
|---|---|
| `README.md` | Member 1 |
| `docs/architecture-decisions.md` | Member 1 |
| `infrastructure/docker-compose.yml` | Member 2 |
| Kafka configuration | Member 2 |
| `postman/` | Member 2 |
| `wso2/` | Member 2 |

The other member may propose changes through pull requests.

---

# Environment and Secrets

Never commit:

- `.env`
- database passwords
- OAuth client secrets
- API keys
- access tokens
- private certificates
- WSO2 secrets
- local application configuration containing credentials

Commit `.env.example` instead.

Example:

```env
POSTGRES_USER=vbank
POSTGRES_PASSWORD=replace_me

USER_SERVICE_URL=http://localhost:8081
ACCOUNT_SERVICE_URL=http://localhost:8082
TRANSACTION_SERVICE_URL=http://localhost:8083

KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Recommended `.gitignore` entries:

```gitignore
target/
.idea/
*.iml
.vscode/
.DS_Store
.env
application-local.properties
application-local.yml
logs/
```

---

# Initial Implementation Order

## Shared Foundation

Complete together:

1. Confirm the decisions in this README.
2. Copy final choices into `docs/architecture-decisions.md`.
3. Confirm Java and Spring Boot versions.
4. Create the five Spring Boot Maven projects.
5. Configure service ports.
6. Create PostgreSQL databases.
7. Add Kafka and PostgreSQL to Docker Compose.
8. Define API contracts.
9. Define the common error response.
10. Define environment variables.

## Member 1 Starts With

1. User registration
2. User login
3. User profile
4. Account creation
5. Account retrieval
6. Atomic account transfer
7. Account inactivity job
8. BFF dashboard

## Member 2 Starts With

1. Transaction entity and initiation
2. Transaction history
3. Transaction execution
4. Kafka infrastructure
5. Logging consumer
6. WSO2 routing and security
7. Gateway Postman collection

---

# Definition of Done

A feature is considered complete when:

- The endpoint or process works.
- Validation is implemented.
- Errors follow the shared format.
- Unit tests pass.
- Integration tests pass where applicable.
- Sensitive data is not logged.
- Configuration uses environment variables.
- Documentation is updated.
- The feature is reviewed through a pull request.
- The branch is merged into `main`.

---

# Current Project Status

```text
[ ] Architecture decisions finalized
[ ] API contracts finalized
[ ] Java/Spring Boot versions finalized
[ ] User Service created
[ ] Account Service created
[ ] Transaction Service created
[ ] BFF Service created
[ ] Logging Service created
[ ] PostgreSQL configured
[ ] Kafka configured
[ ] Docker Compose configured
[ ] WSO2 configured
[ ] Postman collection created
[ ] End-to-end flow tested
```