package com.pagoflex.usuarios.features.profile.controller;

import com.pagoflex.usuarios.features.profile.dto.request.UpdateProfileRequest;
import com.pagoflex.usuarios.features.profile.dto.response.ProfileResponse;
import com.pagoflex.usuarios.features.profile.dto.response.UpdateProfileResponse;
import jakarta.validation.Valid;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Profile")
public interface ProfileController {

    @Operation(summary = "Obtener perfil")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    ResponseEntity<ProfileResponse> getProfile();

    @Operation(summary = "Actualizar perfil")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping
    ResponseEntity<UpdateProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request);

    @Operation(summary = "Descargar avatar")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/avatar")
    ResponseEntity<byte[]> getAvatar();

    @Operation(summary = "Subir avatar")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ProfileResponse> uploadAvatar(@RequestPart("file") MultipartFile file);

    @Operation(summary = "Eliminar avatar")
    @ApiResponse(responseCode = "204", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/avatar")
    ResponseEntity<Void> deleteAvatar();

}
