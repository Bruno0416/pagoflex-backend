package com.pagoflex.usuarios.features.profile.controller;

import com.pagoflex.usuarios.features.profile.dto.request.UpdateProfileRequest;
import com.pagoflex.usuarios.features.profile.dto.response.ProfileResponse;
import com.pagoflex.usuarios.features.profile.dto.response.UpdateProfileResponse;
import com.pagoflex.usuarios.features.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/usuarios/me")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService service;

    // ---------- perfil ----------
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(service.getCurrentProfile());
    }

    @PatchMapping
    public ResponseEntity<UpdateProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(service.updateProfile(request));
    }

    // ---------- avatar ----------
    @GetMapping("/avatar")
    public ResponseEntity<byte[]> getAvatar() {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .body(service.getAvatar());
    }

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadAvatar(file));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> deleteAvatar() {
        service.deleteAvatar();
        return ResponseEntity.noContent().build();
    }
}
