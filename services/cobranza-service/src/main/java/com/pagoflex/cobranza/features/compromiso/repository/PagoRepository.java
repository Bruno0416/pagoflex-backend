package com.pagoflex.cobranza.features.compromiso.repository;

import com.pagoflex.cobranza.features.compromiso.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagoRepository extends JpaRepository<Pago, UUID> {

    // reintento del mismo pago con la misma llave
    @Query("""
            select p from Pago p join fetch p.compromiso c join fetch c.empresa join fetch p.comprobante
            where p.usuarioId = :usuarioId and p.idempotencyKey = :llave
            """)
    Optional<Pago> findPorLlave(@Param("usuarioId") UUID usuarioId, @Param("llave") String llave);

    // historial del usuario, con filtro opcional de empresa y fechas
    @Query("""
            select p from Pago p join fetch p.compromiso c join fetch c.empresa e join fetch p.comprobante
            where c.personaId = :personaId
              and (:empresa is null or e.codigo = :empresa)
              and p.fechaHora >= :desde and p.fechaHora < :hasta
            order by p.fechaHora desc
            """)
    List<Pago> findDePersona(@Param("personaId") UUID personaId,
                             @Param("empresa") String empresa,
                             @Param("desde") LocalDateTime desde,
                             @Param("hasta") LocalDateTime hasta);
}
