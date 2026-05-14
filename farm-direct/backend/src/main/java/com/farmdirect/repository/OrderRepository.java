package com.farmdirect.repository;

import com.farmdirect.model.Order;
import com.farmdirect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerOrderByCreatedAtDesc(User buyer);

    @Query("SELECT o FROM Order o WHERE o.product.farmer = :farmer ORDER BY o.createdAt DESC")
    List<Order> findByFarmer(@Param("farmer") User farmer);
}
