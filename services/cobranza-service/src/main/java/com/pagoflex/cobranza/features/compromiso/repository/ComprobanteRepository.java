package com.pagoflex.cobranza.features.compromiso.repository;

import com.pagoflex.cobranza.features.compromiso.entity.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, UUID> {

    @Query("""
            select cp from Comprobante cp join fetch cp.pago p join fetch p.compromiso c join fetch c.empresa
            where cp.numero = :numero
            """)
    Optional<Comprobante> findByNumero(@Param("numero") String numero);

    @Query("select cp.numero from Comprobante cp where cp.numero like 'CPR-%'")
    List<String> findNumeros();
}
