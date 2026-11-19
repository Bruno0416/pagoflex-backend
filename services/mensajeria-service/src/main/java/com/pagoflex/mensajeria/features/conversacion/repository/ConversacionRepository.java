package com.pagoflex.mensajeria.features.conversacion.repository;

import com.pagoflex.mensajeria.features.conversacion.entity.ConversacionTicket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversacionRepository extends JpaRepository<ConversacionTicket, UUID> {

    Optional<ConversacionTicket> findByReporteNumero(String reporteNumero);

    // bloquea la conversación para dar la siguiente secuencia sin repetir
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConversacionTicket c where c.reporteNumero = :numero")
    Optional<ConversacionTicket> findParaEscribir(@Param("numero") String numero);
}
