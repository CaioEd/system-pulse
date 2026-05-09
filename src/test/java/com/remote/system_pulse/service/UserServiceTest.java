package com.remote.system_pulse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    @DisplayName("Should create user successfully and return DTO")
    void createUser_ShouldReturnDTO_WhenSuccessful() {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO("John Doe", "[EMAIL_ADDRESS]", "+1234567890", "password");
        
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("John Doe");
        savedUser.setEmail("[EMAIL_ADDRESS]");
        savedUser.setPhoneNumber("+1234567890");
        savedUser.setPassword("password");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponseDTO response = userService.createUser(requestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("John Doe", response.name());
        assertEquals("[EMAIL_ADDRESS]", response.email());
        assertEquals("+1234567890", response.phoneNumber());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals("John Doe", capturedUser.getName());
        assertEquals("[EMAIL_ADDRESS]", capturedUser.getEmail());
        assertEquals("+1234567890", capturedUser.getPhoneNumber());
    }

    @Test
    @DisplayName("Should encode password when creating user")
    void createUser_ShouldEncodePassword() {
        // Arrange
        UserRequestDTO requestDTO = new UserRequestDTO("Jane Doe", "[EMAIL_ADDRESS]", "+0987654321", "rawPassword");

        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setName("Jane Doe");
        savedUser.setEmail("[EMAIL_ADDRESS]");
        savedUser.setPhoneNumber("+0987654321");
        savedUser.setPassword("encodedPassword");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        userService.createUser(requestDTO);

        // Assert
        verify(passwordEncoder).encode("rawPassword");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("encodedPassword", userCaptor.getValue().getPassword());
    }

    // ==================== getAllUsers ====================

    @Test
    @DisplayName("Should return a list of all users")
    void getAllUsers_ShouldReturnList() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("[EMAIL_ADDRESS]");
        user.setPhoneNumber("+1234567890");
        user.setPassword("password");

        when(userRepository.findAll()).thenReturn(List.of(user));

        // Act
        List<UserResponseDTO> response = userService.getAllUsers();
 
        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("John Doe", response.get(0).name());
        assertEquals("[EMAIL_ADDRESS]", response.get(0).email());
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should return an empty list when no user is found")
    void getAllUsers_ShouldReturnEmptyList_WhenNoUserIsFound() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of());

        // Act
        List<UserResponseDTO> response = userService.getAllUsers();
 
        // Assert
        assertNotNull(response);
        assertEquals(0, response.size());
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should return multiple users when multiple exist")
    void getAllUsers_ShouldReturnMultipleUsers() {
        // Arrange
        User user1 = new User();
        user1.setId(1L);
        user1.setName("John Doe");
        user1.setEmail("[EMAIL_ADDRESS]");
        user1.setPhoneNumber("+1234567890");

        User user2 = new User();
        user2.setId(2L);
        user2.setName("Jane Doe");
        user2.setEmail("[EMAIL_ADDRESS]");
        user2.setPhoneNumber("+0987654321");

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        // Act
        List<UserResponseDTO> response = userService.getAllUsers();

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("John Doe", response.get(0).name());
        assertEquals("Jane Doe", response.get(1).name());
        verify(userRepository).findAll();
    }

    // ==================== getUserById ====================

    @Test
    @DisplayName("Should return user by ID when found")
    void getUserById_ShouldReturnDTO_WhenFound() {
        // Arrange
        Long id = 1L;
        User user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("[EMAIL_ADDRESS]");
        user.setPhoneNumber("+1234567890");
        user.setPassword("password");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        // Act
        UserResponseDTO response = userService.getUserById(id);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("John Doe", response.name());
        assertEquals("[EMAIL_ADDRESS]", response.email());
        assertEquals("+1234567890", response.phoneNumber());
        verify(userRepository).findById(id);
    }

    @Test
    @DisplayName("Should throw RuntimeException when user not found by ID")
    void getUserById_ShouldThrowException_WhenNotFound() {
        // Arrange
        Long id = 99L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.getUserById(id));

        assertEquals("User not found with id: 99", exception.getMessage());
        verify(userRepository).findById(id);
    }

    // ==================== updateUser ====================

    @Test
    @DisplayName("Should update user successfully and return updated DTO")
    void updateUser_ShouldReturnUpdatedDTO_WhenFound() {
        // Arrange
        Long id = 1L;
        UserRequestDTO requestDTO = new UserRequestDTO("Updated Name", "[EMAIL_ADDRESS]", "+1111111111", "newPassword");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setName("Old Name");
        existingUser.setEmail("[EMAIL_ADDRESS]");
        existingUser.setPhoneNumber("+0000000000");
        existingUser.setPassword("oldEncodedPassword");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("Updated Name");
        updatedUser.setEmail("[EMAIL_ADDRESS]");
        updatedUser.setPhoneNumber("+1111111111");
        updatedUser.setPassword("newEncodedPassword");

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserResponseDTO response = userService.updateUser(id, requestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Updated Name", response.name());
        assertEquals("[EMAIL_ADDRESS]", response.email());
        assertEquals("+1111111111", response.phoneNumber());

        verify(userRepository).findById(id);
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Should set all fields on the existing user entity during update")
    void updateUser_ShouldSetAllFieldsOnExistingUser() {
        // Arrange
        Long id = 1L;
        UserRequestDTO requestDTO = new UserRequestDTO("New Name", "[EMAIL_ADDRESS]", "+9999999999", "secret123");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setName("Old Name");
        existingUser.setEmail("[EMAIL_ADDRESS]");
        existingUser.setPhoneNumber("+0000000000");
        existingUser.setPassword("oldHash");

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("secret123")).thenReturn("hashedSecret");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Act
        userService.updateUser(id, requestDTO);

        // Assert — verify the existing entity was mutated before save
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User captured = userCaptor.getValue();
        assertEquals("New Name", captured.getName());
        assertEquals("[EMAIL_ADDRESS]", captured.getEmail());
        assertEquals("+9999999999", captured.getPhoneNumber());
        assertEquals("hashedSecret", captured.getPassword());
    }

    @Test
    @DisplayName("Should throw RuntimeException when updating a non-existent user")
    void updateUser_ShouldThrowException_WhenNotFound() {
        // Arrange
        Long id = 99L;
        UserRequestDTO requestDTO = new UserRequestDTO("Name", "[EMAIL_ADDRESS]", "+1234567890", "password");

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUser(id, requestDTO));

        assertEquals("User not found with id: 99", exception.getMessage());
        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== deleteUser ====================

    @Test
    @DisplayName("Should delete user successfully when found")
    void deleteUser_ShouldDeleteUser_WhenFound() {
        // Arrange
        Long id = 1L;
        User user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("[EMAIL_ADDRESS]");
        user.setPhoneNumber("+1234567890");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        // Act
        userService.deleteUser(id);

        // Assert
        verify(userRepository).findById(id);
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("Should throw RuntimeException when deleting a non-existent user")
    void deleteUser_ShouldThrowException_WhenNotFound() {
        // Arrange
        Long id = 99L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.deleteUser(id));

        assertEquals("User not found with id: 99", exception.getMessage());
        verify(userRepository).findById(id);
        verify(userRepository, never()).delete(any(User.class));
    }
}
