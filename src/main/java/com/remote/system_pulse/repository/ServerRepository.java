package com.remote.system_pulse.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.remote.system_pulse.model.Server;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {
    // Standard CRUD operations are already implemented save(), findById(), findAll(), deleteById()
}
