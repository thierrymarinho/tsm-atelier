package com.tm.tsm_atelier.domain.user.dto;

import com.tm.tsm_atelier.domain.user.entity.Role;
import java.util.UUID;

public record UserResponseDTO(UUID id, String firstName, String lastName, String name, String email, Role role) {
}
