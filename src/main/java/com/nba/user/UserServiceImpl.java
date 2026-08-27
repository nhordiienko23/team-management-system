package com.nba.user;


import com.nba.core.exception.invalidData.InvalidUserDataException;
import com.nba.core.exception.notFound.UserNotFoundException;
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
                new UserNotFoundException("User with teamName " + username + " not found"));
    }

    @Override
    @Transactional
    public UserDto saveToDataBase(User user) {
        User savedUser = userRepository.save(user);
        return userMapper.toUserDto(savedUser);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getMyProfile(Long userId) {
        return userMapper.toUserDto(getUserById(userId));
    }

    @Override
    @Transactional
    public UserDto partialUpdateUserProfileById(Long userId, UpdateRequest request) {
        User user = getUserById(userId);

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
        return userMapper.toUserDto(user);
    }

    @Override
    @Transactional
    public void deleteUserById(Long userId) {
        userRepository.delete(getUserById(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional
    public void passwordUpdate(Long userId, PasswordUpdateRequest request) {

        User user = getUserById(userId);

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


    private User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() ->
                new UserNotFoundException("User with teamName " + id + " not found"));
    }


}
