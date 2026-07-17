package com.vbank.user_service.controller;

import com.vbank.user_service.dto.request.LoginRequest;
import com.vbank.user_service.dto.request.RegisterUserRequest;
import com.vbank.user_service.dto.response.LoginResponse;
import com.vbank.user_service.dto.response.RegisterUserResponse;
import com.vbank.user_service.dto.response.UserProfileResponse;
import com.vbank.user_service.exception.MissingAuthorizationHeaderException;
import com.vbank.user_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<RegisterUserResponse> register(
            @Valid @RequestBody RegisterUserRequest request
    ) {
        RegisterUserResponse response =
                userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response =
                userService.login(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @PathVariable UUID userId,
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        validateAuthorizationHeader(authorizationHeader);

        UserProfileResponse response =
                userService.getProfile(userId);

        return ResponseEntity.ok(response);
    }

    private void validateAuthorizationHeader(
            String authorizationHeader
    ) {
        if (authorizationHeader == null ||
                authorizationHeader.isBlank()) {

            throw new MissingAuthorizationHeaderException(
                    "Authorization header is required."
            );
        }
    }
}