package com.pagoflex.cobranza.features.codigo.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

/// Misma respuesta para códigos inexistentes, incorrectos, revocados, vencidos o bloqueados:
/// no revela cuál fue el motivo.
public class CodigoInvalido extends PagoFlexException {
    public CodigoInvalido() {
        super(HttpStatus.NOT_FOUND.value(), "El código de empresa no es válido");
    }
}
