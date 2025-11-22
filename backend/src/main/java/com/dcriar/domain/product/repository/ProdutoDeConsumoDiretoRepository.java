package com.dcriar.domain.product.repository;

import com.dcriar.domain.product.entity.ProdutoDeConsumoDireto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoDeConsumoDiretoRepository extends JpaRepository<ProdutoDeConsumoDireto, Long> {
}
