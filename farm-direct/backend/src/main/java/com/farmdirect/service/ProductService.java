package com.farmdirect.service;

import com.farmdirect.dto.ProductDtos.*;
import com.farmdirect.model.Product;
import com.farmdirect.model.User;
import com.farmdirect.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> list(String category, String search) {
        return productRepository.search(
                (category == null || category.isBlank()) ? null : category,
                (search == null || search.isBlank()) ? null : search)
            .stream().map(ProductResponse::from).toList();
    }

    public ProductResponse get(Long id) {
        return ProductResponse.from(find(id));
    }

    public Product find(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public List<ProductResponse> mine(User farmer) {
        return productRepository.findByFarmer(farmer).stream().map(ProductResponse::from).toList();
    }

    public ProductResponse create(ProductRequest req, User farmer) {
        Product p = new Product();
        apply(p, req);
        p.setFarmer(farmer);
        return ProductResponse.from(productRepository.save(p));
    }

    public ProductResponse update(Long id, ProductRequest req, User farmer) {
        Product p = find(id);
        if (!p.getFarmer().getId().equals(farmer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your product");
        }
        apply(p, req);
        return ProductResponse.from(productRepository.save(p));
    }

    public void delete(Long id, User farmer) {
        Product p = find(id);
        if (!p.getFarmer().getId().equals(farmer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your product");
        }
        productRepository.delete(p);
    }

    private void apply(Product p, ProductRequest req) {
        p.setName(req.name);
        p.setCategory(req.category);
        p.setDescription(req.description);
        p.setPricePerUnit(req.pricePerUnit);
        p.setUnit(req.unit);
        p.setQuantityAvailable(req.quantityAvailable);
        p.setHarvestDate(req.harvestDate);
        p.setImageUrl(req.imageUrl);
    }
}
