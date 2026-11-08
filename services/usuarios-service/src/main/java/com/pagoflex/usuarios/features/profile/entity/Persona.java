package com.pagoflex.usuarios.features.profile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "persona")
public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "rut", unique = true, nullable = false)
    private String rut;

    @Column(name = "nombres", nullable = false)
    private String nombres;

    @Column(name = "apellidos", nullable = false)
    private String apellidos;

    /// correo de contacto para usuarios no registrados RN-20
    @Column(name = "correo_contacto")
    private String correoContacto;

    @Column(name = "telefono")
    private String telefono;

    // TODO: agregar campo para avatar
}