package com.farmdirect.controller;

import com.farmdirect.dto.ProductDtos.*;
import com.farmdirect.model.User;
import com.farmdirect.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) String category,
                                      @RequestParam(required = false) String search) {
        return productService.list(category, search);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    public List<ProductResponse> mine(@AuthenticationPrincipal User user) {
        return productService.mine(user);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ProductResponse create(@Valid @RequestBody ProductRequest req,
                                  @AuthenticationPrincipal User user) {
        return productService.create(req, user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ProductResponse update(@PathVariable Long id,
                                  @Valid @RequestBody ProductRequest req,
                                  @AuthenticationPrincipal User user) {
        return productService.update(id, req, user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        productService.delete(id, user);
    }
}
