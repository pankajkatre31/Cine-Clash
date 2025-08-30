package com.cine_clash.user_service.service;

import com.cine_clash.user_service.dto.LoginRequest;
import com.cine_clash.user_service.dto.RefreshRequest;
import com.cine_clash.user_service.dto.SignupRequest;
import com.cine_clash.user_service.entity.User;
import com.cine_clash.user_service.entity.RefreshToken;
import com.cine_clash.user_service.repository.UserRepository;
import com.cine_clash.user_service.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public Map<String, Object> signup(SignupRequest req) {
        if (userRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        User u = userRepo.save(User.builder()
                .email(req.getEmail())
                .passwordHash(encoder.encode(req.getPassword()))
                .role("FREE")
                .build());

        return Map.of("id", u.getId(), "email", u.getEmail());
    }

    public Map<String, String> login(LoginRequest req) {
        User u = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!encoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String jwt = jwtService.generateToken(u);
        String refresh = UUID.randomUUID().toString();

        refreshRepo.deleteByUser(u);
        refreshRepo.save(RefreshToken.builder()
                .user(u)
                .token(refresh)
                .expiry(Instant.now().plus(7, ChronoUnit.DAYS))
                .build());

        return Map.of("accessToken", jwt, "refreshToken", refresh);
    }

    public Map<String, String> refreshToken(RefreshRequest req) {
        RefreshToken rt = refreshRepo.findByToken(req.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (rt.getExpiry().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh expired");
        }

        String jwt = jwtService.generateToken(rt.getUser());
        return Map.of("accessToken", jwt);
    }
}
