package com.nba.user;


import com.nba.core.exception.invalidData.InvalidUserDataException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() ->
                new InvalidUserDataException("User with username " + username + " not found"));
    }

    @Override
    @Transactional
    public UserShortDto saveToDataBase(User user) {
        User savedUser = userRepository.save(user);
        return userMapper.toUserShortDto(savedUser);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public UserShortDto getUserById(Long userId) {
        return userMapper.toUserShortDto(userRepository.findUserByIdOrThrow404(userId));
    }

    @Override
    @Transactional
    public UserShortDto partialUpdateUserProfileById(Long userId, UserUpdateRequest request) {
        User user = userRepository.findUserByIdOrThrow404(userId);

        if (request.username() != null) {
            if (!user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username())) {
                throw new InvalidUserDataException(
                        "User with teamName " + request.username() + " already exists");
            }
            user.setUsername(request.username());
        }

        if (request.email() != null) {
            if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
                throw new InvalidUserDataException(
                        "User with email " + request.email() + " already exists");
            }
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
    public List<UserShortDto> getAllUsers() {
        return userRepository.findAllWithRoles().stream()
                .map(userMapper::toUserShortDto)
                .toList();
    }

    @Override
    @Transactional
    public void passwordUpdateByUserId(Long userId, PasswordUpdateRequest request) {

        User user = userRepository.findUserByIdOrThrow404(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidUserDataException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new InvalidUserDataException("The new password must be different from the current password");
        }

        user.setPassword(
                passwordEncoder.encode(request.newPassword())
        );
    }


    @Override
    @Transactional(readOnly = true)
    public List<UserFullDto> searchUsers(UserSearchFilter filter) {
        return userRepository.findAll(UserSpecification.buildQuery(filter)).stream()
                .map(userMapper::toUserFullDto)
                .toList();
    }

    @Override
    @Transactional
    public UserShortDto createUserAccount(UserCreationRequest request) {
        if (userRepository.existsByUsername(request.username()))
            throw new InvalidUserDataException("User with name " + request.username() + " already exists");
        if (userRepository.existsByEmail(request.email()))
            throw new InvalidUserDataException("User with email " + request.email() + " already exists");
        User newUser = userMapper.toUserEntity(request);
        return userMapper.toUserShortDto(userRepository.save(newUser));
    }


}
