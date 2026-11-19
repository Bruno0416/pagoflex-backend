package com.pagoflex.cobranza.features.compromiso.repository;

import com.pagoflex.cobranza.features.compromiso.entity.Anulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AnulacionRepository extends JpaRepository<Anulacion, UUID> {
}
