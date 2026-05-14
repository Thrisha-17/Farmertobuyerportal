package com.farmdirect.dto;

import com.farmdirect.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public static class RegisterRequest {
        @NotBlank public String name;
        @Email @NotBlank public String email;
        @NotBlank @Size(min = 6) public String password;
        @NotBlank public String phone;
        @NotBlank public String location;
        @NotBlank public String role; // FARMER or BUYER
        public String bio;
    }

    public static class LoginRequest {
        @Email @NotBlank public String email;
        @NotBlank public String password;
    }

    public static class AuthResponse {
        public String token;
        public Long userId;
        public String name;
        public String email;
        public String role;

        public AuthResponse(String token, User u) {
            this.token = token;
            this.userId = u.getId();
            this.name = u.getName();
            this.email = u.getEmail();
            this.role = u.getRole().name();
        }
    }
}
