package com.pagoflex.cobranza.features.compromiso.repository;

import com.pagoflex.cobranza.features.compromiso.entity.Compromiso;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompromisoRepository extends JpaRepository<Compromiso, UUID> {

    @Query("""
            select c from Compromiso c
            join fetch c.empresa left join fetch c.anulacion left join fetch c.pago
            where c.folio = :folio
            """)
    Optional<Compromiso> findByFolio(@Param("folio") String folio);

    // bloquea la fila mientras se paga o cambia de estado
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Compromiso c join fetch c.empresa where c.folio = :folio")
    Optional<Compromiso> findByFolioParaActualizar(@Param("folio") String folio);

    // solo compromisos de la empresa del ejecutivo RN-22
    @Query("""
            select c from Compromiso c
            join fetch c.empresa left join fetch c.anulacion left join fetch c.pago
            where c.empresa.id = :empresaId
              and c.fechaVencimiento between :desde and :hasta
            order by c.fechaVencimiento, c.folio
            """)
    List<Compromiso> findDeEmpresaEntre(@Param("empresaId") UUID empresaId,
                                        @Param("desde") LocalDate desde,
                                        @Param("hasta") LocalDate hasta);

    @Query("""
            select c from Compromiso c
            join fetch c.empresa left join fetch c.anulacion left join fetch c.pago
            where c.empresa.id = :empresaId
            order by c.fechaVencimiento, c.folio
            """)
    List<Compromiso> findDeEmpresa(@Param("empresaId") UUID empresaId);

    // compromisos del usuario final de todas sus empresas
    @Query("""
            select c from Compromiso c
            join fetch c.empresa left join fetch c.anulacion left join fetch c.pago
            where c.personaId = :personaId
            order by c.fechaVencimiento, c.folio
            """)
    List<Compromiso> findDePersona(@Param("personaId") UUID personaId);

    // los de la misma persona y empresa, para respetar el orden de pago RN-04
    @Query("""
            select c from Compromiso c
            where c.personaId = :personaId and c.empresa.id = :empresaId
            order by c.fechaVencimiento, c.numeroCuota, c.folio
            """)
    List<Compromiso> findDePersonaEnEmpresa(@Param("personaId") UUID personaId,
                                            @Param("empresaId") UUID empresaId);

    @Query("select c.folio from Compromiso c where c.folio like concat(:prefijo, '%')")
    List<String> findFoliosConPrefijo(@Param("prefijo") String prefijo);

    // misma persona, empresa, concepto y cuota RN-20
    @Query("""
            select count(c) > 0 from Compromiso c
            where c.personaId = :personaId
              and c.empresa.id = :empresaId
              and lower(trim(c.concepto)) = lower(trim(:concepto))
              and ((:numeroCuota is null and c.numeroCuota is null) or c.numeroCuota = :numeroCuota)
            """)
    boolean existeDuplicado(@Param("personaId") UUID personaId,
                            @Param("empresaId") UUID empresaId,
                            @Param("concepto") String concepto,
                            @Param("numeroCuota") Integer numeroCuota);
}
