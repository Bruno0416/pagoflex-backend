package com.pagoflex.cobranza.features.compromiso.entity;

/// motivos permitidos para anular RN-21; con OTRO el comentario es obligatorio
public enum MotivoAnulacion {
    REPACTACION,
    ERROR_REGISTRO,
    PAGADO_DIRECTO,
    OTRO
}
