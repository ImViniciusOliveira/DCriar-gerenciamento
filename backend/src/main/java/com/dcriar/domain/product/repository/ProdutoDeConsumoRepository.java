package com.dcriar.domain.product.repository;

import com.dcriar.domain.product.entity.ProdutoDeConsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoDeConsumoRepository extends JpaRepository<ProdutoDeConsumo, Long> {
}
