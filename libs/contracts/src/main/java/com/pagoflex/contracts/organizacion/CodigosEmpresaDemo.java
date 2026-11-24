package com.pagoflex.contracts.organizacion;

import java.util.Map;

public final class CodigosEmpresaDemo {
    private static final Map<String, String> CODIGOS = Map.of(
            "INTERNA", "PF00AG",
            "EC-01", "AM01EC",
            "EC-02", "BN02EC",
            "EC-03", "CA03EC",
            "EC-04", "HS04EC",
            "EC-05", "MA05EC",
            "EC-06", "LH06EC",
            "EC-07", "IC07EC",
            "EC-08", "PO08EC"
    );

    private CodigosEmpresaDemo() {
    }

    public static String deEmpresa(String empresa) {
        String codigo = CODIGOS.get(empresa);
        if (codigo == null) throw new IllegalArgumentException("Empresa demo desconocida: " + empresa);
        return codigo;
    }

    public static boolean contiene(String codigo) {
        return codigo != null && CODIGOS.containsValue(codigo);
    }
}
