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


    @Column(name = "correo_contacto")
    private String correoContacto;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "cargo", length = 100)
    private String cargo;

    @Column(name = "avatar", length = 100)
    private String avatar;
}
