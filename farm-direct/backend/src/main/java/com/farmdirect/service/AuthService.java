package com.farmdirect.service;

import com.farmdirect.dto.AuthDtos.*;
import com.farmdirect.model.User;
import com.farmdirect.repository.UserRepository;
import com.farmdirect.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User.Role role;
        try {
            role = User.Role.valueOf(req.role.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be FARMER or BUYER");
        }
        User u = new User();
        u.setName(req.name);
        u.setEmail(req.email.toLowerCase());
        u.setPassword(passwordEncoder.encode(req.password));
        u.setPhone(req.phone);
        u.setLocation(req.location);
        u.setRole(role);
        u.setBio(req.bio);
        userRepository.save(u);
        String token = jwtUtil.generateToken(u.getEmail(), u.getRole().name(), u.getId());
        return new AuthResponse(token, u);
    }

    public AuthResponse login(LoginRequest req) {
        User u = userRepository.findByEmail(req.email.toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(req.password, u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        String token = jwtUtil.generateToken(u.getEmail(), u.getRole().name(), u.getId());
        return new AuthResponse(token, u);
    }
}
