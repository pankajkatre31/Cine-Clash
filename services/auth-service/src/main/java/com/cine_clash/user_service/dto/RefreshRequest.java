package com.cine_clash.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {
    @NotBlank private String refreshToken;
}
