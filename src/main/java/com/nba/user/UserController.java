package com.nba.user;

import com.nba.core.dto.response.MessageResponse;
import com.nba.core.exception.invalidData.InvalidUserDataException;
import com.nba.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User API", description = "Endpoints for managing user profiles")
public class UserController {
    private final UserService userService;

    @Operation(summary = "returns current user profile")
    @GetMapping("/me")
    public ResponseEntity<UserShortDto> getMyProfile(@AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        return ResponseEntity.ok(userService.getUserById(currentUserDetails.getUser().getId()));
    }

    @Operation(summary = "updates current user profile")
    @PatchMapping("/me")
    public ResponseEntity<UserShortDto> updateMyProfile(@AuthenticationPrincipal CustomUserDetails currentUserDetails,
                                                        @Valid @RequestBody UserUpdateRequest updateRequest) {
        return ResponseEntity
                .ok(userService.partialUpdateUserProfileById(
                        currentUserDetails.getUser().getId(),
                        updateRequest));
    }

    @Operation(summary = "delete current user account")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyProfile(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) {
        userService.deleteUserById(customUserDetails.getUser().getId());

        new SecurityContextLogoutHandler().logout(request, response, authentication);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "updates current user password")
    @PatchMapping("/me/update-password")
    public ResponseEntity<MessageResponse> updateMyCurrentPassword(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                   @Valid @RequestBody PasswordUpdateRequest request) {
        userService.passwordUpdateByUserId(customUserDetails.getUser().getId(), request);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Your password was successfully updated")
                .build());
    }

    @Operation(summary = "Returns list of all users")
    @GetMapping
    public ResponseEntity<List<UserShortDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Returns list of user flexibly filtered by any combination of parameters")
    @GetMapping("/search")
    public ResponseEntity<List<UserFullDto>> searchUsers(@ParameterObject UserSearchFilter filter) {
        return ResponseEntity.ok(userService.searchUsers(filter));
    }


    @Operation(summary = "returns user profile by id (for admins)")
    @GetMapping("/{userId}")
    public ResponseEntity<UserShortDto> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @Operation(summary = "updates user profile by user id (for admins)")
    @PatchMapping("/{userId}")
    public ResponseEntity<UserShortDto> updateUserById(@PathVariable Long userId,
                                                       @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.partialUpdateUserProfileById(userId, request));
    }

    @Operation(summary = "delete user account by user id (for admins)")
    @DeleteMapping("/{userId}")
    public ResponseEntity<MessageResponse> deleteUserById(@PathVariable Long userId,
                                                          @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        if (customUserDetails.getUser().getId().equals(userId)) {
            throw new InvalidUserDataException(
                    "Admin cannot delete their own account using this endpoint");
        }
        userService.deleteUserById(userId);
        return ResponseEntity
                .ok(MessageResponse.builder()
                        .message("Profile of user with id " + userId + " was successfully deleted.")
                        .build());
    }

    @Operation(summary = "create new user account (for admins)")
    @PostMapping
    public ResponseEntity<UserShortDto> createNewUser(@Valid @RequestBody UserCreationRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createUserAccount(request));
    }
}
