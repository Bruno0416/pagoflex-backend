package com.pagoflex.usuarios.features.auth.data;

import com.pagoflex.contracts.organization.IdentificadoresEmpresa;
import com.pagoflex.usuarios.core.security.Role;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.profile.entity.Persona;
import com.pagoflex.usuarios.features.profile.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/// Cuentas de ejemplo de la app (anexo 9) con la misma clave demo, para probarla conectada.
/// Solo se cargan con pagoflex.demo.enabled=true; los agentes y ejecutivos quedan asociados a su organización como en el registro con código
@Component
@ConditionalOnProperty(name = "pagoflex.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CargaDemo implements ApplicationRunner {
    private static final String CLAVE_DEMO = "demo123";

    private final UserRepository userRepo;
    private final PersonaRepository personaRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String hash = passwordEncoder.encode(CLAVE_DEMO);

        // 1. usuarios finales
        cuenta(hash, Role.USUARIO_FINAL, "crojas@correo.ejemplo", "15834207-3", "Camila Andrea", "Rojas Peña", "+56 9 0000 0101", null);
        cuenta(hash, Role.USUARIO_FINAL, "lmunoz@correo.ejemplo", "12476318-5", "Luis Alberto", "Muñoz Tapia", "+56 9 0000 0102", null);
        cuenta(hash, Role.USUARIO_FINAL, "rcontreras@correo.ejemplo", "6893541-5", "Rosa Elena", "Contreras Díaz", "+56 9 0000 0103", null);
        cuenta(hash, Role.USUARIO_FINAL, "msoto@correo.ejemplo", "21045873-5", "Matías Ignacio", "Soto Vergara", "+56 9 0000 0104", null);
        cuenta(hash, Role.USUARIO_FINAL, "fgodoy@correo.ejemplo", "17562904-1", "Fernanda Paz", "Godoy Lagos", "+56 9 0000 0105", null);
        cuenta(hash, Role.USUARIO_FINAL, "hpizarro@correo.ejemplo", "10318265-4", "Héctor Manuel", "Pizarro Campos", "+56 9 0000 0106", null);
        cuenta(hash, Role.USUARIO_FINAL, "jreyes@correo.ejemplo", "19287430-0", "Javiera Constanza", "Reyes Molina", "+56 9 0000 0107", null);
        cuenta(hash, Role.USUARIO_FINAL, "baraya@correo.ejemplo", "18704396-4", "Benjamín Esteban", "Araya Fuentes", "+56 9 0000 0108", null);

        // 2. agentes activos
        cuenta(hash, Role.AGENTE, "alagos@pagoflex.ejemplo", "16248317-K", "Andrés Felipe", "Lagos Meza", null, IdentificadoresEmpresa.INTERNA);
        cuenta(hash, Role.AGENTE, "dortiz@pagoflex.ejemplo", "18352091-1", "Daniela Sofía", "Ortiz Bravo", null, IdentificadoresEmpresa.INTERNA);
        cuenta(hash, Role.AGENTE, "vparedes@pagoflex.ejemplo", "17781435-0", "Valentina Ignacia", "Paredes Silva", null, IdentificadoresEmpresa.INTERNA);
        cuenta(hash, Role.AGENTE, "tvidal@pagoflex.ejemplo", "19564028-9", "Tomás Alejandro", "Vidal Rojas", null, IdentificadoresEmpresa.INTERNA);

        // 3. ejecutivos; su empresa es la de su persona (misma fuente que el registro con código)
        cuenta(hash, Role.EJECUTIVO, "psaavedra@altosmaipo.ejemplo", "14629853-2", "Paula Andrea", "Saavedra Núñez", "+56 9 0000 0401", IdentificadoresEmpresa.deCodigoDemo("EC-01"));
        cuenta(hash, Role.EJECUTIVO, "mfuentes@bosquenativo.ejemplo", "11853720-3", "Marco Antonio", "Fuentes Riquelme", "+56 9 0000 0402", IdentificadoresEmpresa.deCodigoDemo("EC-02"));
        cuenta(hash, Role.EJECUTIVO, "chenriquez@creditoandino.ejemplo", "15370946-7", "Claudia Beatriz", "Henríquez Soto", "+56 9 0000 0403", IdentificadoresEmpresa.deCodigoDemo("EC-03"));
        cuenta(hash, Role.EJECUTIVO, "fmorales@creditoandino.ejemplo", "17095482-3", "Felipe Ignacio", "Morales Tapia", "+56 9 0000 0404", IdentificadoresEmpresa.deCodigoDemo("EC-03"));
        cuenta(hash, Role.EJECUTIVO, "gcarrasco@loshalcones.ejemplo", "9846217-1", "Gloria Inés", "Carrasco Vega", "+56 9 0000 0405", IdentificadoresEmpresa.deCodigoDemo("EC-06"));
        cuenta(hash, Role.EJECUTIVO, "snunez@itcordillera.ejemplo", "18930571-0", "Sebastián Andrés", "Núñez Olivares", "+56 9 0000 0406", IdentificadoresEmpresa.deCodigoDemo("EC-07"));
        cuenta(hash, Role.EJECUTIVO, "mleiva@plazaoriente.ejemplo", "12584396-4", "Mónica Patricia", "Leiva Castro", "+56 9 0000 0407", IdentificadoresEmpresa.deCodigoDemo("EC-08"));
    }

    // --- helper privado ---
    private void cuenta(String hash, Role rol, String correo, String rut, String nombres, String apellidos,
                        String telefono, UUID empresaId) {
        // 1. no repetir cuentas que ya existen
        if (userRepo.existsByEmail(correo)) return;

        // 2. guardar la persona y su acceso enlazados
        Persona persona = personaRepo.save(Persona.builder()
                .rut(rut)
                .nombres(nombres)
                .apellidos(apellidos)
                .telefono(telefono)
                .empresaId(empresaId)
                .build());
        userRepo.save(Users.builder()
                .email(correo)
                .password(hash)
                .role(rol)
                .persona(persona)
                .build());
    }
}
