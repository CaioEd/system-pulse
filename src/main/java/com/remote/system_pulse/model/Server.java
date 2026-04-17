package com.remote.system_pulse.model;
import com.remote.system_pulse.model.enums.ServerStatus;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "servers")
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    // Hibernate 6 natively maps to UUID in Postgres
    @Column(nullable = false, unique = true, updatable = false)
    private UUID token;

    // Server's public IP
    private String ip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServerStatus status = ServerStatus.UNKNOWN;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    // dynamic data sent by the remote agent
    
    private Double usageCpu;
    private Double usageRam;
    private Double usageDisk;

    /**
     * Automatically generate the token before the first save to the database.
     * Ensures a server is never persisted without a token.
    */
    @PrePersist
    public void generateToken() {
        if (this.token == null) {
            this.token = UUID.randomUUID();
        }
    }

}