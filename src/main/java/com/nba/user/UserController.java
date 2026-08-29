package com.nba.user;

import com.nba.core.dto.response.MessageResponse;
import com.nba.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User API", description = "Endpoints for managing user profiles")
public class UserController {
    private final UserService userService;

    @Operation(summary = "returns current user profile")
    @GetMapping("/me")
    public ResponseEntity<UserShortDto> getMyProfile(@AuthenticationPrincipal CustomUserDetails currentUserDetails) {
        return ResponseEntity.ok(userService.getUserProfileById(currentUserDetails.getUser().getId()));
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
        userService.passwordUpdateByUserIdForCurrentUser(customUserDetails.getUser().getId(), request);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Your password was successfully updated")
                .build());
    }


}
