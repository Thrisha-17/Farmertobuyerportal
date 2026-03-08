package com.farmconnect.service;

import com.farmconnect.dto.AuthResponse;
import com.farmconnect.dto.LoginRequest;
import com.farmconnect.dto.RegisterRequest;
import com.farmconnect.exception.BusinessException;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import com.farmconnect.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository             userRepository;
    private final FarmerProfileRepository    farmerProfileRepository;
    private final BuyerProfileRepository     buyerProfileRepository;
    private final FarmerDeliveryOptionRepository deliveryOptionRepository;
    private final PasswordEncoder            passwordEncoder;
    private final JwtUtil                    jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new BusinessException("Email already registered: " + request.getEmail());

        if (userRepository.existsByMobile(request.getMobile()))
            throw new BusinessException("Mobile number already registered: " + request.getMobile());

        // Create user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .state(request.getState())
                .build();

        userRepository.save(user);
        log.info("New user registered: {} [{}]", user.getEmail(), user.getRole());

        // Create role-specific profile
        if (request.getRole() == User.Role.FARMER) {
            FarmerProfile profile = FarmerProfile.builder()
                    .user(user)
                    .farmSize(request.getFarmSize())
                    .cropType(request.getCropType())
                    .village(request.getVillage())
                    .build();
            farmerProfileRepository.save(profile);

            // Seed default delivery options for this farmer
            seedDefaultDeliveryOptions(user);

        } else {
            BuyerProfile profile = BuyerProfile.builder()
                    .user(user)
                    .buyerType(request.getBuyerType())
                    .build();
            buyerProfileRepository.save(profile);
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return buildAuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("No account found with this email"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new BusinessException("Incorrect password");

        if (!user.getIsActive())
            throw new BusinessException("Your account has been deactivated. Contact support.");

        log.info("User logged in: {} [{}]", user.getEmail(), user.getRole());

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return buildAuthResponse(token, user);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .role(user.getRole().name())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    /** Create all 6 delivery options (disabled by default) when a farmer registers */
    private void seedDefaultDeliveryOptions(User farmer) {
        List<String> types = List.of(
                "FARM_PICKUP", "LOCAL_DELIVERY", "COURIER",
                "COOPERATIVE_HUB", "SUBSCRIPTION", "COLD_CHAIN"
        );
        types.forEach(type -> {
            FarmerDeliveryOption opt = FarmerDeliveryOption.builder()
                    .farmer(farmer)
                    .deliveryType(type)
                    .isEnabled("FARM_PICKUP".equals(type))  // Pickup enabled by default
                    .deliveryFee(BigDecimal.ZERO)
                    .build();
            deliveryOptionRepository.save(opt);
        });
    }
}
