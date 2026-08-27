package com.nba.user;

import com.nba.core.dto.response.MessageResponse;
import com.nba.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<UserDto> getMyProfile(@AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        return ResponseEntity.ok(userService.getMyProfile(currentUserDetails.getUser().getId()));
    }

    @Operation(summary = "updates current user profile")
    @PatchMapping("/me")
    public ResponseEntity<UserDto> updateMyProfile(@AuthenticationPrincipal CustomUserDetails currentUserDetails,
                                                   @Valid @RequestBody UpdateRequest updateRequest) {
        return ResponseEntity.ok(userService.partialUpdateUserProfileById(
                currentUserDetails.getUser().getId(),
                updateRequest));
    }

    @Operation(summary = "delete current user account")
    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> deleteMyProfile(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        userService.deleteUserById(customUserDetails.getUser().getId());
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Your profile was successfully deleted.")
                .build());
    }

    @Operation(summary = "returns list of all users")
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "updates user current password")
    @PatchMapping("/me/update-password")
    public ResponseEntity<MessageResponse> updateMyCurrentPassword(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                   @Valid @RequestBody PasswordUpdateRequest request) {
        userService.passwordUpdate(customUserDetails.getUser().getId(), request);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Your password was successfully updated")
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResponseSearchUser>> searchUsers(@ParameterObject UserSearchFilter filter) {
        return ResponseEntity.ok(userService.searchUsers(filter));
    }


}
