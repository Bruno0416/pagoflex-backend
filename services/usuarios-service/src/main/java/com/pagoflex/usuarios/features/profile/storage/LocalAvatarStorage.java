package com.pagoflex.usuarios.features.profile.storage;

import com.pagoflex.usuarios.features.profile.exceptions.AvatarNotFound;
import com.pagoflex.usuarios.features.profile.exceptions.AvatarStorageFailure;
import com.pagoflex.usuarios.features.profile.exceptions.InvalidAvatar;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class LocalAvatarStorage implements AvatarStorage {
    private static final Logger LOG = LoggerFactory.getLogger(LocalAvatarStorage.class);
    private static final long MAX_UPLOAD_BYTES = 2L * 1024 * 1024;
    private static final int MAX_SOURCE_DIMENSION = 4096;
    private static final int MAX_OUTPUT_DIMENSION = 256;
    private static final int MAX_OUTPUT_BYTES = 256 * 1024;
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[0-9a-fA-F-]{36}\\.jpg$");
    private static final Set<String> ALLOWED_FORMATS = Set.of("jpeg", "png");
    private final Path directory;

    public LocalAvatarStorage(@Value("${pagoflex.storage.avatars.path:./data/avatars}") String path) {
        this.directory = Path.of(path).toAbsolutePath().normalize();
    }

    // ---------- almacenamiento de avatares ----------
    @Override
    public String save(MultipartFile file) {
        // 1. validar tamano y tipo del archivo
        if (file == null || file.isEmpty() || file.getSize() > MAX_UPLOAD_BYTES) {
            throw new InvalidAvatar("El avatar debe ser una imagen de hasta 2 MiB");
        }
        if (!Set.of("image/jpeg", "image/png").contains(file.getContentType())) {
            throw new InvalidAvatar("Solo se permiten imágenes JPEG o PNG");
        }

        // 2. decodificar y convertir a jpeg
        byte[] encoded = encodeAvatar(file);
        String filename = UUID.randomUUID() + ".jpg";
        Path temp = null;
        try {
            // 3. guardar primero un archivo temporal en el volumen
            Files.createDirectories(directory);
            temp = Files.createTempFile(directory, "avatar-", ".tmp");
            Files.write(temp, encoded);
            // 4. publicar el archivo completo con un cambio atomico de nombre
            try {
                Files.move(temp, directory.resolve(filename), StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, directory.resolve(filename));
            }
            return filename;
        } catch (IOException ex) {
            throw new AvatarStorageFailure();
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ex) {
                    LOG.warn("No se pudo limpiar el archivo temporal {}", temp, ex);
                }
            }
        }
    }

    @Override
    public byte[] read(String filename) {
        try {
            return Files.readAllBytes(resolve(filename));
        } catch (java.nio.file.NoSuchFileException ex) {
            throw new AvatarNotFound();
        } catch (IOException ex) {
            throw new AvatarStorageFailure();
        }
    }

    @Override
    public void delete(String filename) {
        if (filename == null) return;
        try {
            Files.deleteIfExists(resolve(filename));
        } catch (IOException ex) {
            throw new AvatarStorageFailure();
        }
    }

    // --- helpers ---
    private Path resolve(String filename) {
        if (filename == null || !SAFE_FILENAME.matcher(filename).matches()) {
            throw new AvatarNotFound();
        }
        return directory.resolve(filename);
    }

    private byte[] encodeAvatar(MultipartFile file) {
        // 1. comprobar el formato real y limitar dimensiones antes de decodificar
        try (InputStream raw = file.getInputStream();
             ImageInputStream input = ImageIO.createImageInputStream(raw)) {
            if (input == null) throw new InvalidAvatar("La imagen no es válida");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new InvalidAvatar("La imagen no es válida");
            ImageReader reader = readers.next();
            BufferedImage source;
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!ALLOWED_FORMATS.contains(format)) throw new InvalidAvatar("Solo se permiten imágenes JPEG o PNG");
                reader.setInput(input);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_SOURCE_DIMENSION || height > MAX_SOURCE_DIMENSION) {
                    throw new InvalidAvatar("Las dimensiones de la imagen no son válidas");
                }
                source = reader.read(0);
            } finally {
                reader.dispose();
            }
            // 2. reducir el avatar a un maximo de 256 pixeles
            double scale = Math.min(1.0, (double) MAX_OUTPUT_DIMENSION / Math.max(source.getWidth(), source.getHeight()));
            int w = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(source.getHeight() * scale));
            BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = resized.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, w, h);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.drawImage(source, 0, 0, w, h, null);
            } finally {
                graphics.dispose();
            }
            // 3. limitar el tamano resultante ajustando la calidad jpeg
            for (float quality : new float[]{0.8f, 0.6f, 0.4f}) {
                byte[] jpeg = writeJpeg(resized, quality);
                if (jpeg.length <= MAX_OUTPUT_BYTES) return jpeg;
            }
            throw new InvalidAvatar("La imagen procesada supera el tamaño permitido");
        } catch (InvalidAvatar ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new InvalidAvatar("La imagen no se pudo decodificar");
        }
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new IOException("Codificador JPEG no disponible");
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), params);
            imageOutput.flush();
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
