# Account Service Implementation Guide
This guide matches the prepared Account Service implementation for the Virtual Bank System. Java 21 and Spring Boot 4.1.0 are used to match the existing User Service.
## Final design decisions

- Account Service uses a separate PostgreSQL database named `vbank_accounts`, while sharing the existing PostgreSQL container/server.
- Public account creation accepts `SAVINGS` and `CHECKING`; `SYSTEM` remains reserved for the later interest job.
- New accounts start `ACTIVE` with `lastTransactionAt = null`. They are not inactivated until they have participated in at least one transfer and that latest transfer is older than 24 hours.
- No reactivation behavior is implemented yet. Inactive accounts cannot send or receive transfers.
- Account creation validates that the user exists by calling the User Service.
- Transfers are atomic, use `BigDecimal`, and lock both account rows in deterministic UUID order.
## Fastest setup path

1. Copy the prepared `account-service/` folder into the repository root.
2. Copy `infrastructure/postgres/init-databases.sh`.
3. Add the init-script volume shown below to `infrastructure/docker-compose.yml`.
4. Because the existing PostgreSQL volume is already initialized, create `vbank_accounts` once manually:

```bash
cd infrastructure
docker compose up -d postgres
docker exec -it vbank-postgres psql -U vbank -d postgres -c "CREATE DATABASE vbank_accounts OWNER vbank;"
```

If PostgreSQL reports that the database already exists, continue.

5. Start the User Service first, then the Account Service:

```bash
cd user-service
./mvnw spring-boot:run
```

In a second terminal:

```bash
cd account-service
./mvnw spring-boot:run
```
## Folder structure

```text
account-service/
├── .mvn/wrapper/maven-wrapper.properties
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src
    ├── main
    │   ├── java/com/vbank/account_service
    │   │   ├── AccountServiceApplication.java
    │   │   ├── client/UserServiceClient.java
    │   │   ├── config/TimeConfig.java
    │   │   ├── controller/AccountController.java
    │   │   ├── dto/request/...
    │   │   ├── dto/response/...
    │   │   ├── entity/...
    │   │   ├── exception/...
    │   │   ├── repository/AccountRepository.java
    │   │   ├── scheduler/AccountInactivityScheduler.java
    │   │   └── service/...
    │   └── resources/application.yml
    └── test
        ├── java/com/vbank/account_service/AccountServiceApplicationTests.java
        └── resources/application.yml
```
## Manual test order

1. Register a user through the User Service and copy the returned `userId`.
2. Create two accounts for that user.
3. Copy the two returned account IDs.
4. Read each account.
5. List the user accounts.
6. Transfer between the accounts.
7. Read both accounts again and verify both balances changed together.
8. Test validation and exception cases: invalid UUID, missing body, malformed JSON, invalid account type, negative balance, same-account transfer, insufficient funds, unknown account, wrong HTTP method, wrong content type, unknown route, User Service stopped, and unknown user.
## Copy-ready files

### `infrastructure/docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:16
    container_name: vbank-postgres
    restart: unless-stopped

    environment:
      POSTGRES_USER: vbank
      POSTGRES_PASSWORD: vbank_password
      POSTGRES_DB: vbank_users

    ports:
      - "5432:5432"

    volumes:
      - vbank-postgres-data:/var/lib/postgresql/data
      - ./postgres/init-databases.sh:/docker-entrypoint-initdb.d/10-init-databases.sh:ro

    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U vbank -d vbank_users"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  vbank-postgres-data:
```

### `infrastructure/postgres/init-databases.sh`

```bash
#!/usr/bin/env bash
set -e

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname postgres <<-EOSQL
    SELECT 'CREATE DATABASE vbank_accounts OWNER $POSTGRES_USER'
    WHERE NOT EXISTS (
        SELECT FROM pg_database WHERE datname = 'vbank_accounts'
    )\gexec
EOSQL

```

### `account-service/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.0</version>
        <relativePath/>
    </parent>

    <groupId>com.vbank</groupId>
    <artifactId>account-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>account-service</name>
    <description>Account microservice for the Virtual Bank System</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>

        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>

```

### `account-service/src/main/java/com/vbank/account_service/AccountServiceApplication.java`

```java
package com.vbank.account_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/config/TimeConfig.java`

```java
package com.vbank.account_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/entity/AccountType.java`

```java
package com.vbank.account_service.entity;

public enum AccountType {
    SAVINGS,
    CHECKING,
    SYSTEM
}

```

### `account-service/src/main/java/com/vbank/account_service/entity/AccountStatus.java`

```java
package com.vbank.account_service.entity;

public enum AccountStatus {
    ACTIVE,
    INACTIVE
}

```

### `account-service/src/main/java/com/vbank/account_service/entity/Account.java`

```java
package com.vbank.account_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_accounts_account_number",
                        columnNames = "account_number"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "account_id",
            nullable = false,
            updatable = false
    )
    private UUID accountId;

    @Column(
            name = "user_id",
            nullable = false,
            updatable = false
    )
    private UUID userId;

    @Column(
            name = "account_number",
            nullable = false,
            updatable = false,
            length = 10
    )
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "account_type",
            nullable = false,
            updatable = false,
            length = 20
    )
    private AccountType accountType;

    @Column(
            name = "balance",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private AccountStatus status;

    @Column(name = "last_transaction_at")
    private Instant lastTransactionAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    public Account(
            UUID userId,
            String accountNumber,
            AccountType accountType,
            BigDecimal initialBalance
    ) {
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = initialBalance.setScale(2, RoundingMode.UNNECESSARY);
        this.status = AccountStatus.ACTIVE;
        this.lastTransactionAt = null;
    }

    public void debit(
            BigDecimal amount,
            Instant transactionTime
    ) {
        balance = balance.subtract(amount);
        lastTransactionAt = transactionTime;
    }

    public void credit(
            BigDecimal amount,
            Instant transactionTime
    ) {
        balance = balance.add(amount);
        lastTransactionAt = transactionTime;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/dto/request/CreateAccountRequest.java`

```java
package com.vbank.account_service.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(

        @NotNull(message = "User ID is required.")
        UUID userId,

        @NotBlank(message = "Account type is required.")
        @Pattern(
                regexp = "^\\s*(?i:SAVINGS|CHECKING)\\s*$",
                message = "Account type must be SAVINGS or CHECKING."
        )
        String accountType,

        @NotNull(message = "Initial balance is required.")
        @DecimalMin(
                value = "0.00",
                inclusive = true,
                message = "Initial balance must be zero or greater."
        )
        @DecimalMax(
                value = "99999999999999999.99",
                inclusive = true,
                message = "Initial balance is too large."
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Initial balance may contain at most 17 integer digits and 2 decimal places."
        )
        BigDecimal initialBalance
) {
}

```

### `account-service/src/main/java/com/vbank/account_service/dto/request/TransferRequest.java`

```java
package com.vbank.account_service.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(

        @NotNull(message = "From account ID is required.")
        UUID fromAccountId,

        @NotNull(message = "To account ID is required.")
        UUID toAccountId,

        @NotNull(message = "Amount is required.")
        @DecimalMin(
                value = "0.01",
                inclusive = true,
                message = "Amount must be greater than zero."
        )
        @DecimalMax(
                value = "99999999999999999.99",
                inclusive = true,
                message = "Amount is too large."
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Amount may contain at most 17 integer digits and 2 decimal places."
        )
        BigDecimal amount
) {
}

```

### `account-service/src/main/java/com/vbank/account_service/dto/response/CreateAccountResponse.java`

```java
package com.vbank.account_service.dto.response;

import java.util.UUID;

public record CreateAccountResponse(
        UUID accountId,
        String accountNumber,
        String message
) {
}

```

### `account-service/src/main/java/com/vbank/account_service/dto/response/AccountResponse.java`

```java
package com.vbank.account_service.dto.response;

import com.vbank.account_service.entity.AccountStatus;
import com.vbank.account_service.entity.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID accountId,
        String accountNumber,
        AccountType accountType,
        BigDecimal balance,
        AccountStatus status
) {
}

```

### `account-service/src/main/java/com/vbank/account_service/dto/response/TransferResponse.java`

```java
package com.vbank.account_service.dto.response;

public record TransferResponse(
        String message
) {
}

```

### `account-service/src/main/java/com/vbank/account_service/exception/ApiError.java`

```java
package com.vbank.account_service.exception;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}

```

### `account-service/src/main/java/com/vbank/account_service/exception/AccountNotFoundException.java`

```java
package com.vbank.account_service.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/exception/NoAccountsFoundException.java`

```java
package com.vbank.account_service.exception;

public class NoAccountsFoundException extends RuntimeException {

    public NoAccountsFoundException(String message) {
        super(message);
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/exception/UserNotFoundException.java`

```java
package com.vbank.account_service.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/exception/InvalidAccountOperationException.java`

```java
package com.vbank.account_service.exception;

public class InvalidAccountOperationException extends RuntimeException {

    public InvalidAccountOperationException(String message) {
        super(message);
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/exception/InsufficientFundsException.java`

```java
package com.vbank.account_service.exception;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/exception/BalanceLimitExceededException.java`

```java
package com.vbank.account_service.exception;

public class BalanceLimitExceededException extends RuntimeException {

    public BalanceLimitExceededException(String message) {
        super(message);
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/exception/AccountNumberGenerationException.java`

```java
package com.vbank.account_service.exception;

public class AccountNumberGenerationException extends RuntimeException {

    public AccountNumberGenerationException(String message) {
        super(message);
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/exception/UserServiceAuthorizationException.java`

```java
package com.vbank.account_service.exception;

public class UserServiceAuthorizationException extends RuntimeException {

    public UserServiceAuthorizationException(String message) {
        super(message);
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/exception/UserServiceUnavailableException.java`

```java
package com.vbank.account_service.exception;

public class UserServiceUnavailableException extends RuntimeException {

    public UserServiceUnavailableException(String message) {
        super(message);
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/exception/GlobalExceptionHandler.java`

```java
package com.vbank.account_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleAccountNotFound(
            AccountNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(NoAccountsFoundException.class)
    public ResponseEntity<ApiError> handleNoAccountsFound(
            NoAccountsFoundException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler({
            InvalidAccountOperationException.class,
            InsufficientFundsException.class,
            BalanceLimitExceededException.class
    })
    public ResponseEntity<ApiError> handleInvalidAccountOperation(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(UserServiceAuthorizationException.class)
    public ResponseEntity<ApiError> handleUserServiceAuthorization(
            UserServiceAuthorizationException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.BAD_GATEWAY,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleUserServiceUnavailable(
            UserServiceUnavailableException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                "Account data conflicts with an existing record.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler({
            CannotAcquireLockException.class,
            PessimisticLockingFailureException.class,
            ObjectOptimisticLockingFailureException.class
    })
    public ResponseEntity<ApiError> handleConcurrentAccountUpdate(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                "One of the accounts is currently being updated. Please retry the request.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccountNumberGenerationException.class)
    public ResponseEntity<ApiError> handleAccountNumberGeneration(
            AccountNumberGenerationException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));

        return buildError(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "One or more request values are invalid.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        String message = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath()
                        + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        return buildError(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Invalid value for '" + exception.getName() + "'.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        String message =
                "HTTP method '" + exception.getMethod()
                        + "' is not supported for this endpoint.";

        if (exception.getSupportedHttpMethods() != null
                && !exception.getSupportedHttpMethods().isEmpty()) {
            message += " Supported methods: "
                    + exception.getSupportedHttpMethods()
                    + ".";
        }

        return buildError(
                HttpStatus.METHOD_NOT_ALLOWED,
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        String receivedContentType =
                exception.getContentType() == null
                        ? "unknown"
                        : exception.getContentType().toString();

        String supportedContentTypes =
                exception.getSupportedMediaTypes().isEmpty()
                        ? "application/json"
                        : exception.getSupportedMediaTypes().toString();

        String message =
                "Content type '" + receivedContentType
                        + "' is not supported. Supported content types: "
                        + supportedContentTypes + ".";

        return buildError(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.NOT_ACCEPTABLE,
                "The requested response format is not supported.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Malformed or missing JSON request body.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "The requested endpoint was not found.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandlerFound(
            NoHandlerFoundException exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "The requested endpoint was not found.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedError(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred.",
                request.getRequestURI()
        );
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ApiError> buildError(
            HttpStatus status,
            String message,
            String path
    ) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );

        return ResponseEntity.status(status).body(error);
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/repository/AccountRepository.java`

```java
package com.vbank.account_service.repository;

import com.vbank.account_service.entity.Account;
import com.vbank.account_service.entity.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from Account account where account.accountId = :accountId")
    Optional<Account> findByIdForUpdate(
            @Param("accountId") UUID accountId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Account account
               set account.status = :inactiveStatus,
                   account.updatedAt = :updatedAt
             where account.status = :activeStatus
               and account.lastTransactionAt is not null
               and account.lastTransactionAt < :cutoff
            """)
    int markStaleAccountsInactive(
            @Param("activeStatus") AccountStatus activeStatus,
            @Param("inactiveStatus") AccountStatus inactiveStatus,
            @Param("cutoff") Instant cutoff,
            @Param("updatedAt") Instant updatedAt
    );
}

```

### `account-service/src/main/java/com/vbank/account_service/client/UserServiceClient.java`

```java
package com.vbank.account_service.client;

import com.vbank.account_service.exception.UserNotFoundException;
import com.vbank.account_service.exception.UserServiceAuthorizationException;
import com.vbank.account_service.exception.UserServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final String internalAuthorizationHeader;

    public UserServiceClient(
            @Value("${services.user.base-url}") String userServiceBaseUrl,
            @Value("${services.user.internal-authorization}")
            String internalAuthorizationHeader
    ) {
        this.restClient = RestClient.create(userServiceBaseUrl);
        this.internalAuthorizationHeader = internalAuthorizationHeader;
    }

    public void verifyUserExists(UUID userId) {
        try {
            restClient
                    .get()
                    .uri("/users/{userId}/profile", userId)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            internalAuthorizationHeader
                    )
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw new UserNotFoundException(
                        "User with ID " + userId + " not found."
                );
            }

            if (exception.getStatusCode().equals(HttpStatus.UNAUTHORIZED)
                    || exception.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
                throw new UserServiceAuthorizationException(
                        "User Service rejected the Account Service authorization."
                );
            }

            throw new UserServiceUnavailableException(
                    "User Service could not validate the requested user."
            );
        } catch (ResourceAccessException exception) {
            throw new UserServiceUnavailableException(
                    "User Service is currently unavailable."
            );
        }
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/service/AccountNumberGenerator.java`

```java
package com.vbank.account_service.service;

import com.vbank.account_service.exception.AccountNumberGenerationException;
import com.vbank.account_service.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final int ACCOUNT_NUMBER_LENGTH = 10;
    private static final int MAX_GENERATION_ATTEMPTS = 20;

    private final AccountRepository accountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountNumberGenerator(
            AccountRepository accountRepository
    ) {
        this.accountRepository = accountRepository;
    }

    public String generateUniqueAccountNumber() {
        for (int attempt = 0;
             attempt < MAX_GENERATION_ATTEMPTS;
             attempt++) {

            String accountNumber = generateCandidate();

            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }

        throw new AccountNumberGenerationException(
                "A unique account number could not be generated."
        );
    }

    private String generateCandidate() {
        StringBuilder candidate =
                new StringBuilder(ACCOUNT_NUMBER_LENGTH);

        for (int index = 0;
             index < ACCOUNT_NUMBER_LENGTH;
             index++) {
            candidate.append(secureRandom.nextInt(10));
        }

        return candidate.toString();
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/service/AccountService.java`

```java
package com.vbank.account_service.service;

import com.vbank.account_service.client.UserServiceClient;
import com.vbank.account_service.dto.request.CreateAccountRequest;
import com.vbank.account_service.dto.request.TransferRequest;
import com.vbank.account_service.dto.response.AccountResponse;
import com.vbank.account_service.dto.response.CreateAccountResponse;
import com.vbank.account_service.dto.response.TransferResponse;
import com.vbank.account_service.entity.Account;
import com.vbank.account_service.entity.AccountStatus;
import com.vbank.account_service.entity.AccountType;
import com.vbank.account_service.exception.AccountNotFoundException;
import com.vbank.account_service.exception.BalanceLimitExceededException;
import com.vbank.account_service.exception.InsufficientFundsException;
import com.vbank.account_service.exception.InvalidAccountOperationException;
import com.vbank.account_service.exception.NoAccountsFoundException;
import com.vbank.account_service.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private static final BigDecimal MAX_BALANCE =
            new BigDecimal("99999999999999999.99");

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final UserServiceClient userServiceClient;
    private final Clock clock;

    public AccountService(
            AccountRepository accountRepository,
            AccountNumberGenerator accountNumberGenerator,
            UserServiceClient userServiceClient,
            Clock clock
    ) {
        this.accountRepository = accountRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.userServiceClient = userServiceClient;
        this.clock = clock;
    }

    public CreateAccountResponse createAccount(
            CreateAccountRequest request
    ) {
        userServiceClient.verifyUserExists(request.userId());

        AccountType accountType = parsePublicAccountType(
                request.accountType()
        );

        BigDecimal initialBalance = normalizeMoney(
                request.initialBalance()
        );

        String accountNumber =
                accountNumberGenerator.generateUniqueAccountNumber();

        Account account = new Account(
                request.userId(),
                accountNumber,
                accountType,
                initialBalance
        );

        Account savedAccount = accountRepository.save(account);

        return new CreateAccountResponse(
                savedAccount.getAccountId(),
                savedAccount.getAccountNumber(),
                "Account created successfully."
        );
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId) {
        Account account = findAccount(accountId);
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUser(UUID userId) {
        List<Account> accounts =
                accountRepository.findAllByUserIdOrderByCreatedAtAsc(
                        userId
                );

        if (accounts.isEmpty()) {
            throw new NoAccountsFoundException(
                    "No accounts found for user ID " + userId + "."
            );
        }

        return accounts.stream()
                .map(this::toResponse)
                .toList();
    }

    public TransferResponse transfer(TransferRequest request) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new InvalidAccountOperationException(
                    "The source and destination accounts must be different."
            );
        }

        BigDecimal amount = normalizeMoney(request.amount());

        LockedAccounts lockedAccounts = lockAccountsInStableOrder(
                request.fromAccountId(),
                request.toAccountId()
        );

        Account fromAccount = lockedAccounts.fromAccount();
        Account toAccount = lockedAccounts.toAccount();

        validateActive(fromAccount);
        validateActive(toAccount);

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Account " + fromAccount.getAccountId()
                            + " has insufficient funds."
            );
        }

        BigDecimal destinationBalance =
                toAccount.getBalance().add(amount);

        if (destinationBalance.compareTo(MAX_BALANCE) > 0) {
            throw new BalanceLimitExceededException(
                    "The transfer would exceed the destination account balance limit."
            );
        }

        Instant transactionTime = clock.instant();

        fromAccount.debit(amount, transactionTime);
        toAccount.credit(amount, transactionTime);

        return new TransferResponse(
                "Account updated successfully."
        );
    }

    private LockedAccounts lockAccountsInStableOrder(
            UUID fromAccountId,
            UUID toAccountId
    ) {
        UUID firstId;
        UUID secondId;

        if (fromAccountId.compareTo(toAccountId) < 0) {
            firstId = fromAccountId;
            secondId = toAccountId;
        } else {
            firstId = toAccountId;
            secondId = fromAccountId;
        }

        Account firstAccount = findAccountForUpdate(firstId);
        Account secondAccount = findAccountForUpdate(secondId);

        if (firstAccount.getAccountId().equals(fromAccountId)) {
            return new LockedAccounts(firstAccount, secondAccount);
        }

        return new LockedAccounts(secondAccount, firstAccount);
    }

    private Account findAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(
                        () -> accountNotFound(accountId)
                );
    }

    private Account findAccountForUpdate(UUID accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(
                        () -> accountNotFound(accountId)
                );
    }

    private AccountNotFoundException accountNotFound(UUID accountId) {
        return new AccountNotFoundException(
                "Account with ID " + accountId + " not found."
        );
    }

    private void validateActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountOperationException(
                    "Account " + account.getAccountId()
                            + " is inactive and cannot participate in a transfer."
            );
        }
    }

    private AccountType parsePublicAccountType(String value) {
        AccountType accountType = AccountType.valueOf(
                value.trim().toUpperCase(Locale.ROOT)
        );

        if (accountType == AccountType.SYSTEM) {
            throw new InvalidAccountOperationException(
                    "SYSTEM accounts cannot be created through the public account endpoint."
            );
        }

        return accountType;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus()
        );
    }

    private record LockedAccounts(
            Account fromAccount,
            Account toAccount
    ) {
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/service/AccountInactivityService.java`

```java
package com.vbank.account_service.service;

import com.vbank.account_service.entity.AccountStatus;
import com.vbank.account_service.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class AccountInactivityService {

    private static final Duration INACTIVITY_PERIOD =
            Duration.ofHours(24);

    private final AccountRepository accountRepository;
    private final Clock clock;

    public AccountInactivityService(
            AccountRepository accountRepository,
            Clock clock
    ) {
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @Transactional
    public int inactivateStaleAccounts() {
        Instant now = clock.instant();
        Instant cutoff = now.minus(INACTIVITY_PERIOD);

        return accountRepository.markStaleAccountsInactive(
                AccountStatus.ACTIVE,
                AccountStatus.INACTIVE,
                cutoff,
                now
        );
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/scheduler/AccountInactivityScheduler.java`

```java
package com.vbank.account_service.scheduler;

import com.vbank.account_service.service.AccountInactivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AccountInactivityScheduler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AccountInactivityScheduler.class);

    private final AccountInactivityService accountInactivityService;

    public AccountInactivityScheduler(
            AccountInactivityService accountInactivityService
    ) {
        this.accountInactivityService = accountInactivityService;
    }

    @Scheduled(
            cron = "${account.inactivity.cron:0 0 * * * *}",
            zone = "${account.inactivity.zone:UTC}"
    )
    public void inactivateStaleAccounts() {
        int updatedAccounts =
                accountInactivityService.inactivateStaleAccounts();

        LOGGER.info(
                "Account inactivity job completed. Accounts marked inactive: {}",
                updatedAccounts
        );
    }
}

```

### `account-service/src/main/java/com/vbank/account_service/controller/AccountController.java`

```java
package com.vbank.account_service.controller;

import com.vbank.account_service.dto.request.CreateAccountRequest;
import com.vbank.account_service.dto.request.TransferRequest;
import com.vbank.account_service.dto.response.AccountResponse;
import com.vbank.account_service.dto.response.CreateAccountResponse;
import com.vbank.account_service.dto.response.TransferResponse;
import com.vbank.account_service.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping(
            value = "/accounts",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CreateAccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        CreateAccountResponse response =
                accountService.createAccount(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable UUID accountId
    ) {
        return ResponseEntity.ok(
                accountService.getAccount(accountId)
        );
    }

    @GetMapping("/users/{userId}/accounts")
    public ResponseEntity<List<AccountResponse>> getAccountsByUser(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                accountService.getAccountsByUser(userId)
        );
    }

    @PutMapping(
            value = "/accounts/transfer",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request
    ) {
        return ResponseEntity.ok(
                accountService.transfer(request)
        );
    }
}

```

### `account-service/src/main/resources/application.yml`

```yaml
server:
  port: ${ACCOUNT_SERVICE_PORT:8082}

spring:
  application:
    name: account-service

  datasource:
    url: ${ACCOUNT_DB_URL:jdbc:postgresql://localhost:5432/vbank_accounts}
    username: ${ACCOUNT_DB_USERNAME:vbank}
    password: ${ACCOUNT_DB_PASSWORD:vbank_password}
    driver-class-name: org.postgresql.Driver

  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        jdbc:
          time_zone: UTC

services:
  user:
    base-url: ${USER_SERVICE_BASE_URL:http://localhost:8081}
    internal-authorization: ${USER_SERVICE_INTERNAL_AUTHORIZATION:Bearer account-service-internal}

account:
  inactivity:
    cron: ${ACCOUNT_INACTIVITY_CRON:0 0 * * * *}
    zone: ${ACCOUNT_INACTIVITY_ZONE:UTC}

```

### `account-service/src/test/java/com/vbank/account_service/AccountServiceApplicationTests.java`

```java
package com.vbank.account_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AccountServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}

```

### `account-service/src/test/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:vbank_accounts_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false

services:
  user:
    base-url: http://localhost:8081
    internal-authorization: Bearer test-internal

account:
  inactivity:
    cron: "-"
    zone: UTC

```
