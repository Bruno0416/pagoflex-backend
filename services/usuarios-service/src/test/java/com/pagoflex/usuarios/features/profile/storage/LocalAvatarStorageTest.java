package com.pagoflex.usuarios.features.profile.storage;

import com.pagoflex.usuarios.features.profile.exceptions.AvatarNotFound;
import com.pagoflex.usuarios.features.profile.exceptions.InvalidAvatar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class LocalAvatarStorageTest {
    @TempDir Path directory;

    // ---------- escritura y lectura ----------
    @Test
    void convertsPngToSmallJpegWithoutDatabase() throws Exception {
        LocalAvatarStorage storage = new LocalAvatarStorage(directory.toString());
        String filename = storage.save(png(800, 400));
        assertThat(filename).matches("[0-9a-f-]{36}\\.jpg");
        assertThat(Files.exists(directory.resolve(filename))).isTrue();
        byte[] image = storage.read(filename);
        assertThat(image.length).isLessThanOrEqualTo(256 * 1024);
        BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(image));
        assertThat(decoded.getWidth()).isEqualTo(256);
        assertThat(decoded.getHeight()).isEqualTo(128);
        storage.delete(filename);
        assertThat(Files.exists(directory.resolve(filename))).isFalse();
    }

    @Test
    void rejectsBrokenImageAndMissingFile() {
        LocalAvatarStorage storage = new LocalAvatarStorage(directory.toString());
        MockMultipartFile invalid = new MockMultipartFile("file", "fake.png", "image/png", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> storage.save(invalid)).isInstanceOf(InvalidAvatar.class);
        assertThatThrownBy(() -> storage.read("../../secreto.txt")).isInstanceOf(AvatarNotFound.class);
    }

    @Test
    void rejectsOversizedUpload() {
        LocalAvatarStorage storage = new LocalAvatarStorage(directory.toString());
        MockMultipartFile large = new MockMultipartFile("file", "big.png", "image/png", new byte[2 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> storage.save(large)).isInstanceOf(InvalidAvatar.class);
    }

    // --- helper ---
    private MockMultipartFile png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("file", "avatar.png", "image/png", output.toByteArray());
    }
}
