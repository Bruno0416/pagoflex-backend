package com.pagoflex.email.service;

import com.pagoflex.contracts.correo.SolicitudEnvioCorreo;

public interface EmailService {
    void send(SolicitudEnvioCorreo request);
}
