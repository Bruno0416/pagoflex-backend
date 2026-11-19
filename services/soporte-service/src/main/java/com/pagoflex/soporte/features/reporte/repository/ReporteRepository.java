package com.pagoflex.soporte.features.reporte.repository;

import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import com.pagoflex.soporte.features.reporte.entity.Reporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, UUID> {
    Optional<Reporte> findByNumero(String numero);

    Page<Reporte> findByUsuarioIdOrderByFechaCreacionDesc(UUID usuarioId, Pageable pageable);

    // un compromiso solo puede tener un reporte sin resolver
    Optional<Reporte> findFirstByFolioAndEstadoIn(String folio, Collection<EstadoReporte> estados);

    @Query("select r.numero from Reporte r where r.numero like 'REP-%'")
    List<String> findNumeros();

    // bandeja del agente: los abiertos de todos y los que él tomó; primero los atrasados
    @Query(value = """
            select r from Reporte r
            where (r.estado = :abierto or r.agenteId = :agenteId)
              and (:estado is null or r.estado = :estado)
              and (:atrasado is null
                   or (:atrasado = true and r.estado = :abierto and r.fechaCreacion < :limite)
                   or (:atrasado = false and not (r.estado = :abierto and r.fechaCreacion < :limite)))
            order by case when r.estado = :abierto and r.fechaCreacion < :limite then 0 else 1 end,
                     r.fechaCreacion asc
            """,
            countQuery = """
            select count(r) from Reporte r
            where (r.estado = :abierto or r.agenteId = :agenteId)
              and (:estado is null or r.estado = :estado)
              and (:atrasado is null
                   or (:atrasado = true and r.estado = :abierto and r.fechaCreacion < :limite)
                   or (:atrasado = false and not (r.estado = :abierto and r.fechaCreacion < :limite)))
            """)
    Page<Reporte> bandeja(@Param("agenteId") UUID agenteId,
                          @Param("estado") EstadoReporte estado,
                          @Param("atrasado") Boolean atrasado,
                          @Param("limite") LocalDateTime limite,
                          @Param("abierto") EstadoReporte abierto,
                          Pageable pageable);

    // reportes de los cobros de una empresa, los más nuevos primero
    @Query(value = """
            select r from Reporte r
            where r.empresaId = :empresaId and (:estado is null or r.estado = :estado)
            order by r.fechaCreacion desc
            """,
            countQuery = """
            select count(r) from Reporte r
            where r.empresaId = :empresaId and (:estado is null or r.estado = :estado)
            """)
    Page<Reporte> deEmpresa(@Param("empresaId") UUID empresaId,
                            @Param("estado") EstadoReporte estado,
                            Pageable pageable);
}
