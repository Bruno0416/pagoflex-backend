package com.pagoflex.usuarios.features.auth.data;

import com.pagoflex.usuarios.features.auth.dto.request.RegisterRequest;
import com.pagoflex.usuarios.features.auth.exceptions.EmailAlreadyInUse;
import com.pagoflex.usuarios.features.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/// cuentas de ejemplo del anexo 9; solo con pagoflex.demo.enabled=true
@Slf4j
@Component
@ConditionalOnProperty(name = "pagoflex.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CargaDemo implements ApplicationRunner {
    private static final String CLAVE_DEMO = "Demo123!";
    private static final List<RegisterRequest> CUENTAS = List.of(

            // ----------- usuarios finales -----------
            new RegisterRequest("crojas@correo.ejemplo", CLAVE_DEMO, "15834207-3", "Camila Andrea", "Rojas Peña", null),
            new RegisterRequest("lmunoz@correo.ejemplo", CLAVE_DEMO, "12476318-5", "Luis Alberto", "Muñoz Tapia", null),
            new RegisterRequest("rcontreras@correo.ejemplo", CLAVE_DEMO, "6893541-5", "Rosa Elena", "Contreras Díaz", null),
            new RegisterRequest("msoto@correo.ejemplo", CLAVE_DEMO, "21045873-5", "Matías Ignacio", "Soto Vergara", null),
            new RegisterRequest("fgodoy@correo.ejemplo", CLAVE_DEMO, "17562904-1", "Fernanda Paz", "Godoy Lagos", null),
            new RegisterRequest("hpizarro@correo.ejemplo", CLAVE_DEMO, "10318265-4", "Héctor Manuel", "Pizarro Campos", null),
            new RegisterRequest("jreyes@correo.ejemplo", CLAVE_DEMO, "19287430-0", "Javiera Constanza", "Reyes Molina", null),
            new RegisterRequest("baraya@correo.ejemplo", CLAVE_DEMO, "18704396-4", "Benjamín Esteban", "Araya Fuentes", null),

            // ----------- agentes -----------
            new RegisterRequest("alagos@pagoflex.ejemplo", CLAVE_DEMO, "16248317-K", "Andrés Felipe", "Lagos Meza", "PF00AG"),
            new RegisterRequest("dortiz@pagoflex.ejemplo", CLAVE_DEMO, "18352091-1", "Daniela Sofía", "Ortiz Bravo", "PF00AG"),
            new RegisterRequest("vparedes@pagoflex.ejemplo", CLAVE_DEMO, "17781435-0", "Valentina Ignacia", "Paredes Silva", "PF00AG"),
            new RegisterRequest("tvidal@pagoflex.ejemplo", CLAVE_DEMO, "19564028-9", "Tomás Alejandro", "Vidal Rojas", "PF00AG"),

            // ----------- ejecutivos -----------
            new RegisterRequest("psaavedra@altosmaipo.ejemplo", CLAVE_DEMO, "14629853-2", "Paula Andrea", "Saavedra Núñez", "AM01EC"),
            new RegisterRequest("mfuentes@bosquenativo.ejemplo", CLAVE_DEMO, "11853720-3", "Marco Antonio", "Fuentes Riquelme", "BN02EC"),
            new RegisterRequest("chenriquez@creditoandino.ejemplo", CLAVE_DEMO, "15370946-7", "Claudia Beatriz", "Henríquez Soto", "CA03EC"),
            new RegisterRequest("fmorales@creditoandino.ejemplo", CLAVE_DEMO, "17095482-3", "Felipe Ignacio", "Morales Tapia", "CA03EC"),
            new RegisterRequest("gcarrasco@loshalcones.ejemplo", CLAVE_DEMO, "9846217-1", "Gloria Inés", "Carrasco Vega", "LH06EC"),
            new RegisterRequest("snunez@itcordillera.ejemplo", CLAVE_DEMO, "18930571-0", "Sebastián Andrés", "Núñez Olivares", "IC07EC"),
            new RegisterRequest("mleiva@plazaoriente.ejemplo", CLAVE_DEMO, "12584396-4", "Mónica Patricia", "Leiva Castro", "PO08EC")
    );

    private final AuthService authService;

    // ---------- carga inicial ----------
    @Override
    public void run(ApplicationArguments args) {
        for (RegisterRequest request : CUENTAS) {
            try {
                authService.register(request);
            } catch (EmailAlreadyInUse ex) {
                // la cuenta ya existe
            }
        }
        log.info("carga demo: cuentas registradas");
    }
}
