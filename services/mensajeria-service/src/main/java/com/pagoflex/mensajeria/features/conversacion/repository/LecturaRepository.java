package com.pagoflex.mensajeria.features.conversacion.repository;

import com.pagoflex.mensajeria.features.conversacion.entity.LecturaConversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LecturaRepository extends JpaRepository<LecturaConversacion, UUID> {

    Optional<LecturaConversacion> findByConversacion_IdAndUsuarioId(UUID conversacionId, UUID usuarioId);
}
