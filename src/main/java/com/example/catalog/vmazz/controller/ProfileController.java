package com.example.catalog.vmazz.controller;

import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.dto.UserResponse;
import com.example.catalog.vmazz.repository.AppUserRepository;
import com.example.catalog.vmazz.service.CurrentUserService;
import com.example.catalog.vmazz.service.UploadStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final CurrentUserService currentUserService;
    private final AppUserRepository userRepository;
    private final UploadStorageService storage;

    @GetMapping public UserResponse get() { return UserResponse.from(currentUserService.requireUser()); }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse uploadImage(@RequestPart("file") MultipartFile file) {
        AppUser user = currentUserService.requireUser();
        user.setProfileImageUrl(storage.saveImage(file, "profiles"));
        return UserResponse.from(userRepository.save(user));
    }
}
