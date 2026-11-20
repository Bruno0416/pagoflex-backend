package com.pagoflex.usuarios.features.profile.service;

import com.pagoflex.usuarios.features.profile.dto.request.UpdateProfileRequest;
import com.pagoflex.usuarios.features.profile.dto.response.ProfileResponse;
import com.pagoflex.usuarios.features.profile.dto.response.UpdateProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    ProfileResponse getCurrentProfile();

    UpdateProfileResponse updateProfile(UpdateProfileRequest request);

    byte[] getAvatar();

    ProfileResponse uploadAvatar(MultipartFile file);

    void deleteAvatar();
}
