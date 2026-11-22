package com.pagoflex.cobranza.features.empresa.repository;

import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaClienteRepository extends JpaRepository<EmpresaCliente, UUID> {
    Optional<EmpresaCliente> findByCodigo(String codigo);
}
