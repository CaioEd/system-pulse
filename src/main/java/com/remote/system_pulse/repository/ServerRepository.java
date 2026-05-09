package com.remote.system_pulse.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.remote.system_pulse.model.Server;
import com.remote.system_pulse.model.enums.ServerStatus;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {
    Optional<Server> findServerByToken(UUID token);

    /**
     * Returns servers currently flagged ONLINE whose last heartbeat is older than
     * the threshold (or never reported). Filter is pushed to the database so the
     * scheduler does not stream every row over the wire.
     */
    @Query("SELECT s FROM Server s "
         + "WHERE s.status = :onlineStatus "
         + "AND (s.lastHeartbeat IS NULL OR s.lastHeartbeat < :threshold)")
    List<Server> findStaleOnlineServers(
            @Param("onlineStatus") ServerStatus onlineStatus,
            @Param("threshold") LocalDateTime threshold
    );
}
