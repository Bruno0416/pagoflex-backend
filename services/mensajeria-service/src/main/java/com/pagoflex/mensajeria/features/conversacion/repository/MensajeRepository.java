package com.pagoflex.mensajeria.features.conversacion.repository;

import com.pagoflex.mensajeria.features.conversacion.entity.Mensaje;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {

    // del más nuevo al más antiguo, antes del cursor
    @Query("""
            select m from Mensaje m
            where m.conversacion.id = :conversacionId and m.secuencia < :antesDe
            order by m.secuencia desc
            """)
    List<Mensaje> findAnteriores(@Param("conversacionId") UUID conversacionId,
                                 @Param("antesDe") long antesDe,
                                 Pageable pagina);

    // los que llegaron después del último que vio la app (reconexión sse)
    @Query("""
            select m from Mensaje m
            where m.conversacion.id = :conversacionId and m.secuencia > :despuesDe
            order by m.secuencia
            """)
    List<Mensaje> findPosteriores(@Param("conversacionId") UUID conversacionId,
                                  @Param("despuesDe") long despuesDe);

    Optional<Mensaje> findByConversacion_IdAndAutorIdAndClientMessageId(UUID conversacionId, UUID autorId,
                                                                        String clientMessageId);
}
