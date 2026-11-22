package com.pagoflex.mensajeria.features.conversacion.service;

import com.pagoflex.contracts.soporte.ParticipantesReporteInterno;

import com.pagoflex.contracts.cobranza.EmpresaInterna;
import com.pagoflex.core.utils.EmailUtils;
import com.pagoflex.mensajeria.core.client.CobranzaServiceClient;
import com.pagoflex.mensajeria.core.client.SoporteServiceClient;
import com.pagoflex.mensajeria.features.conversacion.dto.request.EnviarMensajeRequest;
import com.pagoflex.mensajeria.features.conversacion.dto.response.ConversacionResponse;
import com.pagoflex.mensajeria.features.conversacion.dto.response.MensajeResponse;
import com.pagoflex.mensajeria.features.conversacion.dto.response.PaginaMensajesResponse;
import com.pagoflex.mensajeria.features.conversacion.entity.ConversacionTicket;
import com.pagoflex.mensajeria.features.conversacion.entity.LecturaConversacion;
import com.pagoflex.mensajeria.features.conversacion.entity.Mensaje;
import com.pagoflex.mensajeria.features.conversacion.entity.RolParticipante;
import com.pagoflex.mensajeria.features.conversacion.exceptions.ConversacionCerrada;
import com.pagoflex.mensajeria.features.conversacion.exceptions.ConversacionSinAgente;
import com.pagoflex.mensajeria.features.conversacion.exceptions.ReporteNoEncontrado;
import com.pagoflex.mensajeria.features.conversacion.exceptions.SinPermisoConversacion;
import com.pagoflex.mensajeria.features.conversacion.repository.ConversacionRepository;
import com.pagoflex.mensajeria.features.conversacion.repository.LecturaRepository;
import com.pagoflex.mensajeria.features.conversacion.repository.MensajeRepository;
import com.pagoflex.mensajeria.features.conversacion.rules.ReglasConversacion;
import com.pagoflex.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversacionServiceImpl implements ConversacionService {
    private final ConversacionRepository conversacionRepo;
    private final MensajeRepository mensajeRepo;
    private final LecturaRepository lecturaRepo;
    private final CreadorConversacion creador;
    private final EventosConversacion eventos;
    private final SoporteServiceClient soporte;
    private final CobranzaServiceClient cobranza;
    private final Clock clock;

    /// participante ya validado contra soporte y cobranza
    private record Acceso(ParticipantesReporteInterno reporte, RolParticipante rol) {
        boolean soloLectura() {
            return ReglasConversacion.soloLectura(reporte.estado());
        }
    }

    // ---------- consultas ----------
    @Override
    @Transactional
    public ConversacionResponse abrir(AuthenticatedUser usuario, String numero) {
        Acceso acceso = validar(usuario, numero);
        ConversacionTicket c = conversacion(numero, acceso);
        long leida = lecturaRepo.findByConversacion_IdAndUsuarioId(c.getId(), usuario.id())
                .map(LecturaConversacion::getUltimaSecuenciaLeida).orElse(0L);
        return new ConversacionResponse(numero, acceso.reporte().estado(), acceso.soloLectura(),
                c.getUltimaSecuencia(), leida, c.getFechaCreacion());
    }

    @Override
    @Transactional
    public PaginaMensajesResponse mensajes(AuthenticatedUser usuario, String numero, Long cursor, Integer limite) {
        ConversacionTicket c = conversacion(numero, validar(usuario, numero));
        int tamano = ReglasConversacion.limite(limite);

        // 1. pedir uno de más para saber si quedan anteriores
        List<Mensaje> pagina = new ArrayList<>(mensajeRepo.findAnteriores(c.getId(),
                cursor == null ? Long.MAX_VALUE : cursor, PageRequest.of(0, tamano + 1)));
        boolean hayMas = pagina.size() > tamano;
        if (hayMas) pagina.remove(pagina.size() - 1);

        // 2. devolver del más antiguo al más nuevo, como se muestran en pantalla
        Collections.reverse(pagina);
        Long anterior = hayMas && !pagina.isEmpty() ? pagina.get(0).getSecuencia() : null;
        return new PaginaMensajesResponse(pagina.stream().map(this::aRespuesta).toList(), anterior);
    }

    // ---------- envío ----------
    @Override
    @Transactional
    public Enviado enviar(AuthenticatedUser usuario, String numero, EnviarMensajeRequest request) {
        Acceso acceso = validar(usuario, numero);
        if (acceso.soloLectura()) throw new ConversacionCerrada();
        conversacion(numero, acceso);

        // 1. bloquear la conversación para tomar la siguiente secuencia
        ConversacionTicket c = conversacionRepo.findParaEscribir(numero).orElseThrow(ReporteNoEncontrado::new);
        String clientId = request.clientMessageId().trim();

        // 2. el mismo mensaje reintentado devuelve el que ya se guardó
        Optional<Mensaje> repetido = mensajeRepo.findByConversacion_IdAndAutorIdAndClientMessageId(
                c.getId(), usuario.id(), clientId);
        if (repetido.isPresent()) return new Enviado(aRespuesta(repetido.get()), false);

        // 3. guardar con fecha y orden del servidor
        c.setUltimaSecuencia(c.getUltimaSecuencia() + 1);
        Mensaje mensaje = mensajeRepo.save(Mensaje.builder()
                .conversacion(c)
                .autorId(usuario.id())
                .autorCorreo(usuario.email() == null ? usuario.id().toString() : EmailUtils.normalize(usuario.email()))
                .rol(acceso.rol())
                .texto(request.texto().trim())
                .enviadoEn(LocalDateTime.now(clock))
                .clientMessageId(clientId)
                .secuencia(c.getUltimaSecuencia())
                .build());
        MensajeResponse respuesta = aRespuesta(mensaje);

        // 4. avisar por sse solo cuando quedó guardado
        alConfirmar(() -> eventos.publicar(numero, respuesta));
        return new Enviado(respuesta, true);
    }

    // ---------- tiempo real ----------
    @Override
    @Transactional
    public SseEmitter eventos(AuthenticatedUser usuario, String numero, Long ultimoVisto) {
        ConversacionTicket c = conversacion(numero, validar(usuario, numero));
        // 1. al reconectar se mandan los que llegaron después del último que vio
        List<MensajeResponse> pendientes = ultimoVisto == null ? List.of()
                : mensajeRepo.findPosteriores(c.getId(), ultimoVisto).stream().map(this::aRespuesta).toList();
        return eventos.abrir(numero, pendientes);
    }

    @Override
    @Transactional
    public void marcarLeido(AuthenticatedUser usuario, String numero, long ultimaSecuencia) {
        ConversacionTicket c = conversacion(numero, validar(usuario, numero));
        long tope = Math.min(ultimaSecuencia, c.getUltimaSecuencia());
        LecturaConversacion lectura = lecturaRepo.findByConversacion_IdAndUsuarioId(c.getId(), usuario.id())
                .orElseGet(() -> LecturaConversacion.builder().conversacion(c).usuarioId(usuario.id()).build());
        // 1. la lectura solo avanza
        lectura.setUltimaSecuenciaLeida(Math.max(lectura.getUltimaSecuenciaLeida(), tope));
        lecturaRepo.save(lectura);
    }

    // --- helpers privados ---
    private Acceso validar(AuthenticatedUser usuario, String numero) {
        // 1. el reporte tiene que existir en soporte
        ParticipantesReporteInterno reporte = soporte.participantes(numero)
                .orElseThrow(ReporteNoEncontrado::new);

        // 2. agente asignado o ejecutivo de la empresa del cobro
        boolean esAgente = usuario.hasRole("AGENTE");
        if (esAgente) {
            if (!ReglasConversacion.agentePuede(reporte.agenteId(), usuario.id())) throw new SinPermisoConversacion();
        } else {
            UUID empresa = cobranza.empresaDeEjecutivo(usuario.id()).map(EmpresaInterna::id).orElse(null);
            if (!ReglasConversacion.ejecutivoPuede(reporte.empresaId(), empresa)) throw new SinPermisoConversacion();
            // 3. hasta que un agente lo tome no hay con quién conversar
            if (!ReglasConversacion.tieneAgente(reporte.agenteId())) throw new ConversacionSinAgente();
        }
        return new Acceso(reporte, ReglasConversacion.rol(esAgente));
    }

    private ConversacionTicket conversacion(String numero, Acceso acceso) {
        // 1. se crea la primera vez y se cierra cuando el reporte se resuelve
        creador.asegurar(numero, acceso.reporte().empresaId());
        ConversacionTicket c = conversacionRepo.findByReporteNumero(numero).orElseThrow(ReporteNoEncontrado::new);
        if (acceso.soloLectura() && c.getFechaCierre() == null) c.setFechaCierre(LocalDateTime.now(clock));
        return c;
    }

    private MensajeResponse aRespuesta(Mensaje m) {
        return new MensajeResponse(m.getId(), m.getConversacion().getReporteNumero(), m.getAutorId(),
                m.getAutorCorreo(), m.getRol(), m.getTexto(), m.getEnviadoEn(), m.getClientMessageId(),
                m.getSecuencia());
    }

    private static void alConfirmar(Runnable accion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            accion.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }
}
