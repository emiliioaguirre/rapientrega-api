package com.universidad.rapientrega.repositories;

import java.util.List;
import com.universidad.rapientrega.models.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByRestauranteId(Long restauranteId);
}
