package com.pagoflex.contracts.cobranza;

import java.util.UUID;

/// Empresa cliente a la que pertenece un ejecutivo.
public record EmpresaInterna(UUID id, String codigo, String nombre) {
}
