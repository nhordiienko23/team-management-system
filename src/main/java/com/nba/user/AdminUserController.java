package com.nba.user;

import com.nba.core.dto.response.MessageResponse;
import com.nba.core.exception.invalidData.InvalidUserDataException;
import com.nba.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "User API", description = "Endpoints for managing user profiles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserService userService;

    @Operation(summary = "Returns list of all users ")
    @GetMapping
    public ResponseEntity<Page<UserShortDto>> getAllUsers(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @Operation(summary = "Returns list of user flexibly filtered by any combination of parameters ")
    @GetMapping("/search")
    public ResponseEntity<Page<UserFullDto>> searchUsers(@ParameterObject UserSearchFilter filter,
                                                         @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(userService.searchUsers(filter, pageable));
    }


    @Operation(summary = "returns user profile by id")
    @GetMapping("/{userId}")
    public ResponseEntity<UserShortDto> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfileById(userId));
    }

    @Operation(summary = "updates user profile by user id ")
    @PatchMapping("/{userId}")
    public ResponseEntity<UserShortDto> updateUserById(@PathVariable Long userId,
                                                       @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.partialUpdateUserProfileById(userId, request));
    }

    @Operation(summary = "delete user account by user id ")
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

    @Operation(summary = "create new user account ")
    @PostMapping
    public ResponseEntity<UserShortDto> createNewUser(@Valid @RequestBody UserCreationRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createUserAccount(request));
    }

    @Operation(summary = "Sets new password for user by user id ")
    @PatchMapping("/{userId}/updateUserPassword")
    public ResponseEntity<MessageResponse> setUserPassword(@PathVariable Long userId,
                                                           @Valid @RequestBody PasswordUpdateRequestForAdmins request) {
        userService.setUserPasswordById(userId, request.newPassword());
        return ResponseEntity
                .ok(MessageResponse.builder()
                        .message("New password was successfully set for user with id " + userId)
                        .build());
    }

}
