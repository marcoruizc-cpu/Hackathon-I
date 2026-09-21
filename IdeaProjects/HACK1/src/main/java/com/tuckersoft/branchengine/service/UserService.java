package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.UpdateRoleRequest;
import com.tuckersoft.branchengine.dto.UserResponse;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> VALID_ROLES = Set.of("ROLE_USER", "ROLE_ADMIN");

    private final UserRepository userRepository;

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    public UserResponse getCurrentUser(String email) {
        return toResponse(getByEmail(email));
    }

    public List<UserResponse> listAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse updateRole(Long id, UpdateRoleRequest request, String actingAdminEmail) {
        if (!VALID_ROLES.contains(request.getRole())) {
            throw ApiException.badRequest("El role debe ser ROLE_USER o ROLE_ADMIN");
        }

        User target = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        if (target.getEmail().equalsIgnoreCase(actingAdminEmail)) {
            throw ApiException.badRequest("Un administrador no puede cambiar su propio rol");
        }

        target.setRole(request.getRole());
        userRepository.save(target);
        return toResponse(target);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
