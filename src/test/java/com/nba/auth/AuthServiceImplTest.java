package com.nba.auth;

import com.nba.core.exception.invalidData.InvalidUserDataException;
import com.nba.user.User;
import com.nba.user.UserService;
import com.nba.user.UserShortDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private AuthServiceImpl authService;


    @Test
    @DisplayName("should register user")
    void shouldRegisterUser() {
        RegisterRequest registerRequest = createRegisterRequest();
        User user = createUser();

        UserShortDto expectedUserShortDTO = createUserShortDTO();

        when(authMapper.toUserEntity(registerRequest))
                .thenReturn(user);

        when(userService.saveToDataBaseAndReturnDto(user))
                .thenReturn(expectedUserShortDTO);

        UserShortDto result = authService.register(registerRequest);

        assertEquals(expectedUserShortDTO, result);

        verify(userService).validateUsernameIsFree(registerRequest.username());

        verify(userService).validateEmailIsFree(registerRequest.email());

        verify(authMapper).toUserEntity(registerRequest);

        verify(userService).saveToDataBaseAndReturnDto(user);
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when username already exists")
    void shouldThrowInvalidUserDataExceptionWhenUsernameAlreadyExists() {
        RegisterRequest registerRequest = createRegisterRequest();

        doThrow(new InvalidUserDataException(
                "User with username " + registerRequest.username() + " already exists"))

                .when(userService).validateUsernameIsFree(registerRequest.username());

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
                () -> authService.register(registerRequest));

        assertEquals("User with username " + registerRequest.username() + " already exists", exception.getMessage());

        verify(userService).validateUsernameIsFree(registerRequest.username());

        verify(userService, never()).validateEmailIsFree(anyString());

        verify(authMapper, never()).toUserEntity(any(RegisterRequest.class));

        verify(userService, never()).saveToDataBaseAndReturnDto(any(User.class));

    }

    @Test
    @DisplayName("should throw InvalidUserDataException when email already exists")
    void shouldThrowInvalidUserDataExceptionWhenEmailAlreadyExists() {
        RegisterRequest registerRequest = createRegisterRequest();

        doThrow(new InvalidUserDataException("User with email " + registerRequest.email() + " already exists"))
                .when(userService).validateEmailIsFree(registerRequest.email());

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
                () -> authService.register(registerRequest));

        assertEquals("User with email " + registerRequest.email() + " already exists", exception.getMessage());

        verify(userService).validateUsernameIsFree(registerRequest.username());

        verify(userService).validateEmailIsFree(registerRequest.email());

        verify(authMapper, never())
                .toUserEntity(any(RegisterRequest.class));

        verify(userService, never())
                .saveToDataBaseAndReturnDto(any(User.class));

    }


    private RegisterRequest createRegisterRequest() {
        return RegisterRequest.builder()
                .username("nikita")
                .email("nikita@gmail.com")
                .password("password")
                .build();
    }

    private User createUser() {
        return User.builder()
                .id(1L)
                .username("nikita")
                .email("nikita@gmail.com")
                .build();
    }

    private UserShortDto createUserShortDTO() {
        return UserShortDto.builder()
                .id(1L)
                .username("nikita")
                .email("nikita@gmail.com")
                .build();
    }

}