package com.remote.system_pulse.service;

import com.remote.system_pulse.dto.ServerRequestDTO;
import com.remote.system_pulse.dto.ServerResponseDTO;
import com.remote.system_pulse.model.Server;
import com.remote.system_pulse.repository.ServerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerServiceTest {

    @Mock
    private ServerRepository serverRepository;

    @InjectMocks
    private ServerService serverService;

    @Test
    @DisplayName("Should create a server successfully and return DTO")
    void createServer_ShouldReturnDTO_WhenSuccessful() {
        // Arrange
        ServerRequestDTO requestDTO = new ServerRequestDTO("Alpha Server", "Production Server");
        
        Server savedServer = new Server();
        savedServer.setId(1L);
        savedServer.setName("Alpha Server");
        savedServer.setDescription("Production Server");

        when(serverRepository.save(any(Server.class))).thenReturn(savedServer);

        // Act
        ServerResponseDTO response = serverService.createServer(requestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());

        // Capturing the argument to ensure the service mapped the DTO correctly before saving
        ArgumentCaptor<Server> serverCaptor = ArgumentCaptor.forClass(Server.class);
        verify(serverRepository).save(serverCaptor.capture());
        
        Server capturedServer = serverCaptor.getValue();
        assertEquals("Alpha Server", capturedServer.getName());
        assertEquals("Production Server", capturedServer.getDescription());
    }

    @Test
    @DisplayName("Should return server by ID when found")
    void getServerById_ShouldReturnDTO_WhenFound() {
        // Arrange
        Long serverId = 1L;
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
        // Verify that no other interaction happened with the repo
        verify(serverRepository).findById(serverId);
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
        verify(serverRepository).findAll();
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
        existingServer.setDescription("Old Description");

        when(serverRepository.findById(id)).thenReturn(Optional.of(existingServer));
        // We mock save to return whatever is passed to it, to allow flow continuation
        when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ServerResponseDTO response = serverService.updateServer(id, updateRequest);

        // Assert
        assertNotNull(response);
        
        // Capturing the argument to ensure the service actually updated the entity fields
        ArgumentCaptor<Server> serverCaptor = ArgumentCaptor.forClass(Server.class);
        verify(serverRepository).save(serverCaptor.capture());

        Server capturedServer = serverCaptor.getValue();
        assertEquals("New Name", capturedServer.getName());
        assertEquals("New Description", capturedServer.getDescription());
        assertEquals(id, capturedServer.getId());
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