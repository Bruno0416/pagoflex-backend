package com.pagoflex.soporte.features.reporte.storage;

import com.pagoflex.soporte.features.reporte.exceptions.ComprobanteInvalido;
import com.pagoflex.soporte.features.reporte.exceptions.ComprobanteNoEncontrado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/// guarda las fotos de comprobante en disco, igual que los avatares de usuarios-service
@Component
public class ComprobanteStorage {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONES = Map.of("image/jpeg", ".jpg", "image/png", ".png");
    private static final Pattern NOMBRE_SEGURO = Pattern.compile("^[0-9a-fA-F-]{36}[.](jpg|png)$");

    private final Path directorio;

    public ComprobanteStorage(@Value("${pagoflex.storage.comprobantes.path:./data/comprobantes}") String path) {
        this.directorio = Path.of(path).toAbsolutePath().normalize();
    }

    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > MAX_BYTES) {
            throw new ComprobanteInvalido("El comprobante debe ser una imagen de hasta 5 MB");
        }
        String extension = EXTENSIONES.get(archivo.getContentType());
        if (extension == null) {
            throw new ComprobanteInvalido("Solo se aceptan imágenes JPEG o PNG");
        }
        String nombre = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(directorio);
            Files.write(directorio.resolve(nombre), archivo.getBytes());
            return nombre;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public record Archivo(byte[] datos, String tipo) {
    }

    public Archivo leer(String nombre) {
        if (nombre == null || !NOMBRE_SEGURO.matcher(nombre).matches()) {
            throw new ComprobanteNoEncontrado();
        }
        Path archivo = directorio.resolve(nombre).normalize();
        if (!archivo.startsWith(directorio) || !Files.exists(archivo)) {
            throw new ComprobanteNoEncontrado();
        }
        try {
            return new Archivo(Files.readAllBytes(archivo), tipo(nombre));
        } catch (IOException e) {
            throw new ComprobanteNoEncontrado();
        }
    }

    static String tipo(String nombre) {
        return nombre != null && nombre.endsWith(".png") ? "image/png" : "image/jpeg";
    }
}
