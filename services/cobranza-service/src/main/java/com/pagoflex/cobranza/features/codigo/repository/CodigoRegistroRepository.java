package com.pagoflex.cobranza.features.codigo.repository;

import com.pagoflex.cobranza.features.codigo.entity.CodigoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodigoRegistroRepository extends JpaRepository<CodigoRegistro, UUID> {
    Optional<CodigoRegistro> findByPrefijo(String prefijo);

    boolean existsByPrefijo(String prefijo);
}
