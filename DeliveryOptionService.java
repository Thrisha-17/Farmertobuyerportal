// ── DeliveryOptionService.java ──────────────────────────────
package com.farmconnect.service;

import com.farmconnect.dto.DeliveryOptionRequest;
import com.farmconnect.exception.ResourceNotFoundException;
import com.farmconnect.exception.UnauthorizedException;
import com.farmconnect.model.FarmerDeliveryOption;
import com.farmconnect.model.User;
import com.farmconnect.repository.FarmerDeliveryOptionRepository;
import com.farmconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryOptionService {

    private final FarmerDeliveryOptionRepository deliveryRepo;
    private final UserRepository                 userRepository;

    public List<FarmerDeliveryOption> getOptions(Long farmerId) {
        return deliveryRepo.findByFarmerId(farmerId);
    }

    public List<FarmerDeliveryOption> getEnabledOptions(Long farmerId) {
        return deliveryRepo.findByFarmerIdAndIsEnabledTrue(farmerId);
    }

    @Transactional
    public FarmerDeliveryOption updateOption(Long farmerId, DeliveryOptionRequest request) {
        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", farmerId));

        if (farmer.getRole() != User.Role.FARMER)
            throw new UnauthorizedException("Only farmers can manage delivery options");

        FarmerDeliveryOption option = deliveryRepo
                .findByFarmerIdAndDeliveryType(farmerId, request.getDeliveryType())
                .orElse(FarmerDeliveryOption.builder().farmer(farmer).build());

        option.setDeliveryType(request.getDeliveryType());
        if (request.getIsEnabled()    != null) option.setIsEnabled(request.getIsEnabled());
        if (request.getDeliveryFee()  != null) option.setDeliveryFee(request.getDeliveryFee());
        if (request.getMaxDistance()  != null) option.setMaxDistance(request.getMaxDistance());
        if (request.getNotes()        != null) option.setNotes(request.getNotes());

        return deliveryRepo.save(option);
    }
}
