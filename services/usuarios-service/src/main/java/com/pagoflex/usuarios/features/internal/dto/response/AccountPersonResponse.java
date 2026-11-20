package com.pagoflex.usuarios.features.internal.dto.response;

import java.util.UUID;

/// Persona de una cuenta: lo que cobranza y soporte necesitan a partir del sub del JWT
public record AccountPersonResponse(UUID idUsuario, UUID idPersona, String rut, String nombres, String apellidos,
                                    String correo, String telefono, UUID idEmpresa, String rol) {}
