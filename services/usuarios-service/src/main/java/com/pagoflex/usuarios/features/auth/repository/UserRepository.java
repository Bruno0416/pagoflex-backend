package com.pagoflex.usuarios.features.auth.repository;

import com.pagoflex.usuarios.features.auth.entity.Users;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {
    @EntityGraph(attributePaths = "persona")
    Optional<Users> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = "persona")
    Optional<Users> findById(UUID id);

    Optional<Users> findByPersona_Id(UUID personaId);

    boolean existsByEmail(String email);

    boolean existsByPersona_Id(UUID personaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from users u where u.id = :id")
    Optional<Users> findByIdForUpdate(@Param("id") UUID id);
}
