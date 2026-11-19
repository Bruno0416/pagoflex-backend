package com.pagoflex.cobranza.features.compromiso.repository;

import com.pagoflex.cobranza.features.compromiso.entity.Anulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnulacionRepository extends JpaRepository<Anulacion, UUID> {

    // anulaciones que el usuario ve en su historial
    @Query("""
            select a from Anulacion a
            join fetch a.compromiso c join fetch c.empresa
            where c.personaId = :personaId
            order by a.fecha desc
            """)
    List<Anulacion> findDePersona(@Param("personaId") UUID personaId);
}
