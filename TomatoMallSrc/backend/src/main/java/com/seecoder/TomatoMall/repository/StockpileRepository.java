package com.seecoder.TomatoMall.repository;

import com.seecoder.TomatoMall.po.Stockpile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StockpileRepository extends JpaRepository<Stockpile, Integer> {
    Optional<Stockpile> findByProductId(Integer productId);

    void deleteByProductId(Integer id);
}
