package com.seecoder.TomatoMall.repository;

import com.seecoder.TomatoMall.po.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpecificationRepository extends JpaRepository<Specification, Integer> {
    List<Specification> findByProductId(Integer productId);

    void deleteByProductId(Integer id);
}
