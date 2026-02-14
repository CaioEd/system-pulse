package com.remote.system_pulse.service;

import com.remote.system_pulse.dto.ServerRequestDTO;
import com.remote.system_pulse.dto.ServerResponseDTO;
import com.remote.system_pulse.model.Server;
import com.remote.system_pulse.repository.ServerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito with JUnit 5
class ServerServiceTest {

    @Mock
    private ServerRepository serverRepository; // Mock Repository to simulate database behavior

    @InjectMocks
    private ServerService serverService; // Real Service injecting the mock

    @Test
    @DisplayName("Should create a server successfully and return DTO")
    void createServer_ShouldReturnDTO_WhenSuccessful() {
        // Arrange
        ServerRequestDTO requestDTO = new ServerRequestDTO("Alpha Server", "Production Server");
        
        // Simulating the object that the database would save (with a generated ID)
        Server savedServer = new Server();
        savedServer.setId(1L);
        savedServer.setName("Alpha Server");
        savedServer.setDescription("Production Server");

        // when save methos is called, returns the object simulated
        when(serverRepository.save(any(Server.class))).thenReturn(savedServer);

        // real service method is called
        ServerResponseDTO response = serverService.createServer(requestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id()); // Verify if ID was mapped correctly
        assertEquals("Alpha Server", response.name()); // Verify if Name was mapped correctly
        
        // Verify if the repository save method was called exactly once
        verify(serverRepository, times(1)).save(any(Server.class));
    }

    @Test
    @DisplayName("Should return server by ID when found")
    void getServerById_ShouldReturnDTO_WhenFound() {
        // Arrange
        Long serverId = 1L; // 1L defines the mock id as 1
        Server server = new Server();
        server.setId(serverId);
        server.setName("Beta Server");

        when(serverRepository.findById(serverId)).thenReturn(Optional.of(server));

        // Act
        ServerResponseDTO response = serverService.getServerById(serverId);

        // Assert
        assertEquals("Beta Server", response.name());
        verify(serverRepository).findById(serverId);
    }

    @Test
    @DisplayName("Should throw exception when server is not found by ID")
    void getServerById_ShouldThrowException_WhenNotFound() {
        // Arrange
        Long serverId = 99L;
        when(serverRepository.findById(serverId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            serverService.getServerById(serverId);
        });

        assertEquals("Server not found with id: 99", exception.getMessage());
    }

    @Test
    @DisplayName("Should return a list of all servers")
    void getAllServers_ShouldReturnList() {
        // Arrange
        Server s1 = new Server(); s1.setName("Server 01");
        Server s2 = new Server(); s2.setName("Server 02");
        Server s3 = new Server(); s3.setName("Server 03");
        
        when(serverRepository.findAll()).thenReturn(List.of(s1, s2, s3));

        // Act
        List<ServerResponseDTO> result = serverService.getAllServers();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Server 01", result.get(0).name());
        assertEquals("Server 02", result.get(1).name());
        assertEquals("Server 03", result.get(2).name());
    }

    @Test
    @DisplayName("Should update server when it exists")
    void updateServer_ShouldUpdateAndReturnDTO() {
        // Arrange
        Long id = 1L;
        ServerRequestDTO updateRequest = new ServerRequestDTO("New Name", "New Description");
        
        Server existingServer = new Server();
        existingServer.setId(id);
        existingServer.setName("Old Name");

        when(serverRepository.findById(id)).thenReturn(Optional.of(existingServer));
        // Mocking save to return the same object passed as argument (updated entity)
        when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ServerResponseDTO response = serverService.updateServer(id, updateRequest);

        // Assert
        assertEquals("New Name", response.name());
        assertEquals("New Description", response.description());
    }

    @Test
    @DisplayName("Should delete server when it exists")
    void deleteServer_ShouldCallDelete_WhenFound() {
        // Arrange
        Long id = 1L;
        Server server = new Server();
        server.setId(id);

        when(serverRepository.findById(id)).thenReturn(Optional.of(server));

        // Act
        serverService.deleteServer(id);

        // Assert
        verify(serverRepository).delete(server);
    }
}