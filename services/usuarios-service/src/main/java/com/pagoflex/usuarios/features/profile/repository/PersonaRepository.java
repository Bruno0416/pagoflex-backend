package com.pagoflex.usuarios.features.profile.repository;

import com.pagoflex.usuarios.features.profile.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PersonaRepository extends JpaRepository<Persona, UUID> {

    Optional<Persona> findByRut(String rut);

    boolean existsByRut(String rut);
}
