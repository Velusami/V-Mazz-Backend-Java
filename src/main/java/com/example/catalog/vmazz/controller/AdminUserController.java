package com.example.catalog.vmazz.controller;

import com.example.catalog.vmazz.dto.CreateSubUserRequest;
import com.example.catalog.vmazz.dto.PageResponse;
import com.example.catalog.vmazz.dto.UpdateUserRequest;
import com.example.catalog.vmazz.dto.UpdateUserStatusRequest;
import com.example.catalog.vmazz.dto.UserResponse;
import com.example.catalog.vmazz.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "4. Company users (admin only)", description = "The admin creates and manages sub users")
public class AdminUserController {

    private final UserAdminService userAdminService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a sub user for the company")
    public UserResponse create(@Valid @RequestBody CreateSubUserRequest request) {
        return userAdminService.createSubUser(request);
    }

    @GetMapping
    @Operation(summary = "List the company's users")
    public PageResponse<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userAdminService.list(Paging.of(page, size, Sort.by("id")));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userAdminService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user's name and mobile")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userAdminService.update(id, request);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a sub user")
    public UserResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return userAdminService.updateStatus(id, request.status());
    }
}
