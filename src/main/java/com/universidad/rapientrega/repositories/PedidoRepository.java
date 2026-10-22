package com.universidad.rapientrega.repositories;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import com.universidad.rapientrega.models.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> buscarConBloqueo(@Param("id") Long id);
    List<Pedido> findByEstadoAndLimiteAceptacionLessThanEqual(Pedido.Estado estado, LocalDateTime ahora);
    @Query("select p.id from Pedido p where p.repartidor.id = :repartidorId and p.estado = :estado")
    List<Long> buscarIdsPorRepartidorYEstado(@Param("repartidorId") Long repartidorId,
            @Param("estado") Pedido.Estado estado);
}
