package com.pagoflex.reconocimientos.features.recognition.service;

import com.pagoflex.contracts.cobranza.PaymentFactsResponse;
import com.pagoflex.contracts.vinculacion.EvaluateRecognitionsRequest;
import com.pagoflex.contracts.vinculacion.EvaluateRecognitionsResponse;
import com.pagoflex.security.CurrentUser;
import com.pagoflex.reconocimientos.core.exceptions.CobranzaInvalidData;
import com.pagoflex.reconocimientos.core.exceptions.CobranzaUnavailable;
import com.pagoflex.reconocimientos.core.client.CobranzaClient;
import com.pagoflex.reconocimientos.features.recognition.domain.PaymentFactsValidator;
import com.pagoflex.reconocimientos.features.recognition.domain.PaymentRules;
import com.pagoflex.reconocimientos.features.recognition.dto.response.UserRecognitionResponse;
import com.pagoflex.reconocimientos.features.recognition.repository.UserRecognitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecognitionService {
    private final UserRecognitionRepository repository;
    private final RecognitionGrantService grants;
    private final CobranzaClient cobranza;
    private final Clock clock;

    // ---------- reconocimientos del usuario ----------
    public List<UserRecognitionResponse> mine() {
        UUID userId = CurrentUser.id();
        // 1. al consultar, evaluar los mismos logros que la app local evaluaba antes de mostrar
        try {
            evaluate(new EvaluateRecognitionsRequest(userId, null));
        } catch (CobranzaUnavailable | CobranzaInvalidData ex) {
            // 2. una caida del proveedor no debe ocultar logros ya obtenidos.
            log.warn("No se pudo actualizar los reconocimientos de {}: {}", userId,
                    ex.getClass().getSimpleName());
        }
        // 3. consultar solo los reconocimientos del usuario autenticado
        return repository.findByUserIdOrderByObtainedAtDescIdDesc(userId).stream()
                .map(row -> new UserRecognitionResponse(row.getCode().name(),
                        row.getCode().displayName(), row.getCode().description(), row.getReason(),
                        row.getObtainedAt().atZone(clock.getZone()).toOffsetDateTime()))
                .toList();
    }

    // ---------- evaluacion desde cobranza ----------
    public EvaluateRecognitionsResponse evaluate(EvaluateRecognitionsRequest request) {
        // 1. consultar y validar hechos de pago directamente en cobranza
        PaymentFactsResponse facts = cobranza.fetchPaymentFacts(request.idUsuario());
        PaymentFactsValidator.validate(facts, request.idUsuario());

        // 2. evaluar solamente las dos reglas disponibles en la app
        var candidates = PaymentRules.evaluate(facts.compromisos(), LocalDate.now(clock), clock.getZone());

        // 3. guardar cada logro una sola vez, incluso si la senal se repite
        List<String> granted = candidates.stream()
                .filter(candidate -> grants.grantIfAbsent(request.idUsuario(), candidate))
                .map(candidate -> candidate.code().name())
                .toList();
        return new EvaluateRecognitionsResponse(request.idUsuario(), granted, OffsetDateTime.now(clock));
    }
}
