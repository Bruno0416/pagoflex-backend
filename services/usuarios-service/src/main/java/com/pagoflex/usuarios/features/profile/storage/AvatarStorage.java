package com.pagoflex.usuarios.features.profile.storage;

import org.springframework.web.multipart.MultipartFile;

public interface AvatarStorage {
    String save(MultipartFile file);

    byte[] read(String filename);

    void delete(String filename);
}
