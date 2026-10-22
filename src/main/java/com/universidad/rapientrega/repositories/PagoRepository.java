package com.universidad.rapientrega.repositories;

import java.util.List;
import java.util.Optional;
import com.universidad.rapientrega.models.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByPedidoId(Long pedidoId);
    List<Pago> findByEstado(Pago.Estado estado);
}
