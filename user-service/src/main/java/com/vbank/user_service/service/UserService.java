package com.vbank.user_service.service;

import com.vbank.user_service.dto.LogMessage;
import com.vbank.user_service.dto.MessageType;
import com.vbank.user_service.dto.request.LoginRequest;
import com.vbank.user_service.dto.request.RegisterUserRequest;
import com.vbank.user_service.dto.response.LoginResponse;
import com.vbank.user_service.dto.response.RegisterUserResponse;
import com.vbank.user_service.dto.response.UserProfileResponse;
import com.vbank.user_service.entity.User;
import com.vbank.user_service.exception.InvalidCredentialsException;
import com.vbank.user_service.exception.UserAlreadyExistsException;
import com.vbank.user_service.exception.UserNotFoundException;
import com.vbank.user_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;
import java.time.Clock;


@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoggingProducerService loggingProducerService;
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            LoggingProducerService loggingProducerService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loggingProducerService = loggingProducerService;
    }

    /**
     * Registers a new user.
     *
     * @param request registration data
     * @return registered user information
     */
    public RegisterUserResponse register(
            RegisterUserRequest request
    ) {
        String normalizedUsername =
                normalizeUsername(request.username());

        String normalizedEmail =
                normalizeEmail(request.email());

        boolean usernameExists =
                userRepository.existsByUsernameIgnoreCase(
                        normalizedUsername
                );

        boolean emailExists =
                userRepository.existsByEmailIgnoreCase(
                        normalizedEmail
                );

        if (usernameExists || emailExists) {
            logOperation(
                    "Username or email already exists tried to register.",
                    "POST",
                    "/users/register",
                    400,
                    null
            );
            throw new UserAlreadyExistsException(
                    "Username or email already exists."
            );
        }

        String passwordHash =
                passwordEncoder.encode(request.password());

        User user = new User(
                normalizedUsername,
                passwordHash,
                normalizedEmail,
                request.firstName().trim(),
                request.lastName().trim()
        );

        User savedUser = userRepository.save(user);

        logOperation(
                "User registered successfully.",
                "POST",
                "/users/register",
                201,
                savedUser.getUserId()
        );

        return new RegisterUserResponse(
                savedUser.getUserId(),
                savedUser.getUsername(),
                "User registered successfully."
        );
    }

    /**
     * Validates a user's login credentials.
     *
     * @param request username and password
     * @return basic authenticated user information
     */
    @Transactional(readOnly = true)
    public LoginResponse login(
            LoginRequest request
    ) {
        String normalizedUsername =
                normalizeUsername(request.username());

        User user = userRepository
                .findByUsernameIgnoreCase(normalizedUsername)
                .orElseThrow(this::invalidCredentials);

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            throw invalidCredentials();
        }

        logOperation(
                "User logged in successfully.",
                "POST",
                "/users/login",
                200,
                user.getUserId()
        );

        return new LoginResponse(
                user.getUserId(),
                user.getUsername()
        );
    }

    /**
     * Retrieves the profile of an existing user.
     *
     * @param userId user UUID
     * @return user profile data
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(
            UUID userId
    ) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new UserNotFoundException(
                                "User with ID "
                                        + userId
                                        + " not found."
                        )
                );

        logOperation(
                "User profile retrieved successfully.",
                "GET",
                "/users/{userId}/profile",
                200,
                user.getUserId()
        );

        return new UserProfileResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
    }
    private void logOperation(
            String message,
            String httpMethod,
            String path,
            Integer statusCode,
            UUID correlationId
    ) {
        Clock clock = Clock.systemUTC();
        loggingProducerService.send(
                LogMessage.builder()
                        .message(message)
                        .messageType(MessageType.REQUEST)
                        .dateTime(clock.instant())
                        .serviceName("user-service")
                        .httpMethod(httpMethod)
                        .path(path)
                        .statusCode(statusCode)
                        .correlationId(correlationId)
                        .appName("Virtual Bank")
                        .build()
        );
    }
    private InvalidCredentialsException invalidCredentials() {
        return new InvalidCredentialsException(
                "Invalid username or password."
        );
    }

    private String normalizeUsername(
            String username
    ) {
        return username
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(
            String email
    ) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
