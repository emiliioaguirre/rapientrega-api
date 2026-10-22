package com.universidad.rapientrega.repositories;

import com.universidad.rapientrega.models.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {
}
