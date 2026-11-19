package com.pagoflex.soporte.features.reporte.repository;

import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import com.pagoflex.soporte.features.reporte.entity.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, UUID> {
    Optional<Reporte> findByNumero(String numero);

    List<Reporte> findByCorreoUsuarioIgnoreCaseOrderByFechaCreacionDesc(String correoUsuario);

    List<Reporte> findAllByOrderByFechaCreacionAsc();

    // un compromiso solo puede tener un reporte sin resolver
    Optional<Reporte> findFirstByFolioAndEstadoIn(String folio, Collection<EstadoReporte> estados);

    @Query("select r.numero from Reporte r where r.numero like 'REP-%'")
    List<String> findNumeros();
}
