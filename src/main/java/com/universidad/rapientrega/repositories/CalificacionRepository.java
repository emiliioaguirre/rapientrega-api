package com.universidad.rapientrega.repositories;

import com.universidad.rapientrega.models.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {
    boolean existsByPedidoId(Long pedidoId);
}
