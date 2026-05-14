package com.farmdirect.repository;

import com.farmdirect.model.Product;
import com.farmdirect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByFarmer(User farmer);

    @Query("SELECT p FROM Product p WHERE " +
            "(:category IS NULL OR LOWER(p.category) = LOWER(:category)) AND " +
            "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "p.quantityAvailable > 0 " +
            "ORDER BY p.createdAt DESC")
    List<Product> search(@Param("category") String category, @Param("search") String search);
}
