package com.pagoflex.usuarios.features.profile.repository;

import com.pagoflex.usuarios.features.profile.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.UUID;

public interface PersonaRepository extends JpaRepository<Persona, UUID> {
    boolean existsByRut(String rut);
    java.util.Optional<Persona> findByRut(String rut);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Persona p where p.rut = :rut")
    java.util.Optional<Persona> findByRutForUpdate(@Param("rut") String rut);
}
