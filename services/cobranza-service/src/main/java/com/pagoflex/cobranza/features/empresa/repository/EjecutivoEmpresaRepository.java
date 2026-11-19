package com.pagoflex.cobranza.features.empresa.repository;

import com.pagoflex.cobranza.features.empresa.entity.EjecutivoEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EjecutivoEmpresaRepository extends JpaRepository<EjecutivoEmpresa, UUID> {

    @Query("select e from EjecutivoEmpresa e join fetch e.empresa where e.usuarioId = :usuarioId")
    Optional<EjecutivoEmpresa> findConEmpresa(@Param("usuarioId") UUID usuarioId);
}
