package com.pagoflex.usuarios.features.auth.data;

import com.pagoflex.usuarios.features.auth.dto.request.RegisterRequest;
import com.pagoflex.usuarios.features.auth.exceptions.EmailAlreadyInUse;
import com.pagoflex.usuarios.features.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CargaDemoTest {
    private final AuthService auth = mock(AuthService.class);

    @Test
    void createsTheAppAccountsWithTheDemoPassword() {
        new CargaDemo(auth).run(null);

        ArgumentCaptor<RegisterRequest> peticiones = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(auth, times(19)).register(peticiones.capture());
        assertThat(peticiones.getAllValues()).allMatch(r -> r.contrasena().equals("Demo123!"));
        assertThat(peticiones.getAllValues()).filteredOn(r -> r.codigoEmpresa() == null).hasSize(8);
        assertThat(peticiones.getAllValues()).filteredOn(r -> "PF00AG".equals(r.codigoEmpresa())).hasSize(4);
        assertThat(peticiones.getAllValues()).filteredOn(r -> r.codigoEmpresa() != null && !"PF00AG".equals(r.codigoEmpresa()))
                .extracting(RegisterRequest::codigoEmpresa)
                .containsExactly("AM01EC", "BN02EC", "CA03EC", "CA03EC", "LH06EC", "IC07EC", "PO08EC");
    }

    @Test
    void doesNotRepeatExistingAccounts() {
        doAnswer(invocation -> {
            RegisterRequest request = invocation.getArgument(0);
            throw new EmailAlreadyInUse(request.correo());
        }).when(auth).register(any(RegisterRequest.class));

        new CargaDemo(auth).run(null);

        verify(auth, times(19)).register(any(RegisterRequest.class));
    }
}
