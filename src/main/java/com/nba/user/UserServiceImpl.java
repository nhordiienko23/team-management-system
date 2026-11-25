package com.nba.user;


import com.nba.core.exception.invalidData.InvalidUserDataException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final String CUSTOM_AUTH_ISSUER = "http://localhost:9000";

    @Value("${app.oauth2.custom-oidc-issuer:http://localhost:9000}")
    private String customAuthIssuer = CUSTOM_AUTH_ISSUER;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ExternalIdentityRepository externalIdentityRepository;

    @Override
    public void validateUsernameIsFree(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new InvalidUserDataException("User with username " + username + " already exists");
        }
    }

    @Override
    public void validateEmailIsFree(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new InvalidUserDataException("User with email " + email + " already exists");
        }
    }

    @Override
    @Transactional
    public void setUserPasswordById(Long userId, String newPassword) {
        User user = userRepository.findUserByIdOrThrow404(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() ->
                new InvalidUserDataException("User with username " + username + " not found"));
    }

    @Override
    @Transactional
    public UserShortDto saveToDataBaseAndReturnDto(User user) {
        User savedUser = userRepository.save(user);
        return userMapper.toUserShortDto(savedUser);
    }


    @Override
    @Transactional(readOnly = true)
    public UserShortDto getUserProfileById(Long userId) {
        return userMapper.toUserShortDto(userRepository.findUserByIdOrThrow404(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailDto getAdminUserDetailsById(Long userId) {
        User user = userRepository.findUserByIdOrThrow404(userId);
        UserFullDto details = userMapper.toUserFullDto(user);
        List<String> linkedProviders = externalIdentityRepository.findAllByUser_Id(userId).stream()
                .map(ExternalIdentity::getProvider)
                .map(this::providerDisplayName)
                .distinct()
                .sorted()
                .toList();

        return new AdminUserDetailDto(
                details.id(),
                details.username(),
                details.email(),
                details.roles(),
                details.registeredAt(),
                details.lastLogin(),
                linkedProviders
        );
    }

    @Override
    @Transactional
    public UserShortDto partialUpdateUserProfileById(Long userId, UserUpdateRequest request) {
        User user = userRepository.findUserByIdOrThrow404(userId);

        if (request.username() != null && !user.getUsername().equals(request.username())) {
            validateUsernameIsFree(request.username());
            user.setUsername(request.username());
        }

        if (request.email() != null && !user.getEmail().equals(request.email())) {
            validateEmailIsFree(request.email());
            user.setEmail(request.email());
        }

        
        return userMapper.toUserShortDto(user);
    }

    @Override
    @Transactional
    public void deleteUserById(Long userId) {
        userRepository.delete(userRepository.findUserByIdOrThrow404(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserShortDto> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(userMapper::toUserShortDto);
    }

    @Override
    @Transactional
    public void passwordUpdateByUserIdForCurrentUser(Long userId, PasswordUpdateRequest request) {

        User user = userRepository.findUserByIdOrThrow404(userId);

        boolean hasRealPassword = user.getPassword() != null && user.getPassword().startsWith("$2");

        if (hasRealPassword) {
            if (request.currentPassword() == null) {
                throw new InvalidUserDataException("Current password is required");
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new InvalidUserDataException("Current password is incorrect");
            }

            if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
                throw new InvalidUserDataException("The new password must be different from the current password");
            }
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }


    @Override
    @Transactional(readOnly = true)
    public Page<UserFullDto> searchUsers(UserSearchFilter filter,Pageable pageable) {
        Page<UserFullDto> users = userRepository.findAll(UserSpecification.buildQuery(filter),pageable)
                .map(userMapper::toUserFullDto);

        List<Long> userIds = users.getContent().stream()
                .map(UserFullDto::id)
                .toList();
        if (userIds.isEmpty()) {
            return users;
        }

        Map<Long, List<String>> providersByUserId = externalIdentityRepository
                .findAllByUser_IdIn(userIds).stream()
                .collect(Collectors.groupingBy(
                        identity -> identity.getUser().getId(),
                        Collectors.mapping(
                                identity -> providerDisplayName(identity.getProvider()),
                                Collectors.collectingAndThen(
                                        Collectors.toSet(),
                                        names -> names.stream().sorted().toList()
                                )
                        )
                ));

        return users.map(user -> new UserFullDto(
                user.id(),
                user.username(),
                user.email(),
                user.roles(),
                user.registeredAt(),
                user.lastLogin(),
                providersByUserId.getOrDefault(user.id(), List.of())
        ));
    }

    private String providerDisplayName(String provider) {
        if (customAuthIssuer.equals(provider)) {
            return "Custom authorization server";
        }
        return switch (provider) {
            case "google", GOOGLE_ISSUER -> "Google";
            case "github" -> "GitHub";
            case "my-custom-auth", CUSTOM_AUTH_ISSUER -> "Custom authorization server";
            default -> "Other sign-in provider";
        };
    }

    @Override
    @Transactional
    public UserShortDto createUserAccount(UserCreationRequest request) {
        validateUsernameIsFree(request.username());
        validateEmailIsFree(request.email());
        User newUser = userMapper.toUserEntity(request);
        return userMapper.toUserShortDto(userRepository.save(newUser));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findWithRolesByEmail(email);
    }

    @Override
    public User saveToDatabase(User user) {
        return userRepository.save(user);
    }

    @Override
    public User createOAuth2User(String username, String email) {
        String uniqueUsername = username;

        if (userRepository.existsByUsername(username)) {
            String shortUuid = java.util.UUID.randomUUID().toString().substring(0, 5);
            uniqueUsername = username + "_" + shortUuid;
        }

        User newUser = User.builder()
                .username(uniqueUsername)
                .email(email)
                .roles(Set.of(UserRole.ROLE_USER))
                .registeredAt(LocalDateTime.now())
                .build();

        return saveToDatabase(newUser);
    }

    @Override
    public User findByUsernameOrEmail(String login) {
        return userRepository.findByUsernameOrEmail(login)
                .orElseThrow(() -> new InvalidUserDataException("User not found with login/email: " + login));
    }


}
