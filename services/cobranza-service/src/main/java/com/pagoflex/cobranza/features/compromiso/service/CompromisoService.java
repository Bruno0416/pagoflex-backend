package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.cobranza.features.compromiso.dto.request.AnularCompromisoRequest;
import com.pagoflex.cobranza.features.compromiso.dto.request.RegistrarCompromisoRequest;
import com.pagoflex.cobranza.features.compromiso.dto.response.CompromisoResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.PersonaEncontradaResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.ResumenCobranzaResponse;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.empresa.dto.response.EmpresaResponse;

import java.time.YearMonth;
import java.util.List;

public interface CompromisoService {
    CompromisoResponse registrar(String ejecutivo, RegistrarCompromisoRequest request);

    List<CompromisoResponse> listar(String ejecutivo, YearMonth mes, EstadoCompromiso estado);

    CompromisoResponse detalle(String ejecutivo, String folio);

    CompromisoResponse anular(String ejecutivo, String folio, AnularCompromisoRequest request);

    ResumenCobranzaResponse resumen(String ejecutivo, YearMonth mes);

    EmpresaResponse empresa(String ejecutivo);

    PersonaEncontradaResponse buscarPersona(String ejecutivo, String rut);

    // usuario final: solo sus propios compromisos
    List<CompromisoResponse> deUsuario(String correo);
}
