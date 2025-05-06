package com.seecoder.TomatoMall.repository;

import com.seecoder.TomatoMall.po.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ProductRepository extends JpaRepository<Product, Integer>
{
    //
    @Query("SELECT p FROM Product p WHERE LOWER(p.title) LIKE %:keyword% OR LOWER(p.description) LIKE %:keyword%")
    List<Product> searchProducts(@Param("keyword") String keyword);
}
