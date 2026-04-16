package com.remote.system_pulse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.remote.system_pulse.dto.UserRequestDTO;
import com.remote.system_pulse.dto.UserResponseDTO;
import com.remote.system_pulse.model.User;
import com.remote.system_pulse.repository.UserRepository;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should create a user successfully and return DTO")
    void createUser_ShouldReturnDTO_WhenSuccessful() {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO("John Doe", "john.doe@example.com", "+1234567890", "password123");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("John Doe");
        savedUser.setEmail("john.doe@example.com");
        savedUser.setPhoneNumber("+1234567890");
        savedUser.setPassword("encodedPassword");

        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponseDTO response = userService.createUser(requestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("John Doe", response.name());
        assertEquals("john.doe@example.com", response.email());
        assertEquals("+1234567890", response.phoneNumber());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals("John Doe", capturedUser.getName());
        assertEquals("john.doe@example.com", capturedUser.getEmail());
        assertEquals("+1234567890", capturedUser.getPhoneNumber());
        assertEquals("encodedPassword", capturedUser.getPassword());
    }

    @Test
    @DisplayName("Should return user by ID when found")
    void getUserById_ShouldReturnDTO_WhenFound() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");
        user.setPhoneNumber("+1234567890");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        UserResponseDTO response = userService.getUserById(userId);

        // Assert
        assertEquals(userId, response.id());
        assertEquals("John Doe", response.name());
        assertEquals("john.doe@example.com", response.email());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw exception when user is not found by ID")
    void getUserById_ShouldThrowException_WhenNotFound() {
        // Arrange
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserById(userId);
        });

        assertEquals("User not found with id: 99", exception.getMessage());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should return a list of all users")
    void getAllUsers_ShouldReturnList() {
        // Arrange
        User u1 = new User(); u1.setName("Alice"); u1.setEmail("alice@example.com");
        User u2 = new User(); u2.setName("Bob");   u2.setEmail("bob@example.com");
        User u3 = new User(); u3.setName("Carol");  u3.setEmail("carol@example.com");

        when(userRepository.findAll()).thenReturn(List.of(u1, u2, u3));

        // Act
        List<UserResponseDTO> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Alice", result.get(0).name());
        assertEquals("Bob",   result.get(1).name());
        assertEquals("Carol", result.get(2).name());
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should update user when it exists")
    void updateUser_ShouldUpdateAndReturnDTO() {
        // Arrange
        Long id = 1L;
        UserRequestDTO updateRequest = new UserRequestDTO("Jane Doe", "jane.doe@example.com", "+0987654321", "newpassword");

        User existingUser = new User();
        existingUser.setId(id);
        existingUser.setName("John Doe");
        existingUser.setEmail("john.doe@example.com");
        existingUser.setPhoneNumber("+1234567890");
        existingUser.setPassword("oldEncodedPassword");

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponseDTO response = userService.updateUser(id, updateRequest);

        // Assert
        assertNotNull(response);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals("Jane Doe", capturedUser.getName());
        assertEquals("jane.doe@example.com", capturedUser.getEmail());
        assertEquals("+0987654321", capturedUser.getPhoneNumber());
        assertEquals("newEncodedPassword", capturedUser.getPassword());
        assertEquals(id, capturedUser.getId());
    }

    @Test
    @DisplayName("Should throw exception when updating a non-existent user")
    void updateUser_ShouldThrowException_WhenNotFound() {
        // Arrange
        Long id = 99L;
        UserRequestDTO updateRequest = new UserRequestDTO("Jane Doe", "jane.doe@example.com", "+0987654321", "newpassword");

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUser(id, updateRequest);
        });

        assertEquals("User not found with id: 99", exception.getMessage());
        verify(userRepository).findById(id);
    }

    @Test
    @DisplayName("Should delete user when it exists")
    void deleteUser_ShouldCallDelete_WhenFound() {
        // Arrange
        Long id = 1L;
        User user = new User();
        user.setId(id);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        // Act
        userService.deleteUser(id);

        // Assert
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("Should throw exception when deleting a non-existent user")
    void deleteUser_ShouldThrowException_WhenNotFound() {
        // Arrange
        Long id = 99L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.deleteUser(id);
        });

        assertEquals("User not found with id: 99", exception.getMessage());
        verify(userRepository).findById(id);
    }
}
