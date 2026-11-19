package com.pagoflex.cobranza.features.compromiso.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/// Persona a la que se le cobra; el ejecutivo la registra si aún no existe RN-20
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "persona")
public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "rut", unique = true, nullable = false, length = 12)
    private String rut;

    @Column(name = "nombres", nullable = false)
    private String nombres;

    @Column(name = "apellidos", nullable = false)
    private String apellidos;

    /// mismo correo con que la persona entra a la app; así ve sus compromisos
    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }
}
