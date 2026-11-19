package com.pagoflex.cobranza.features.compromiso.repository;

import com.pagoflex.cobranza.features.compromiso.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, UUID> {
    Optional<Persona> findByRut(String rut);
}
