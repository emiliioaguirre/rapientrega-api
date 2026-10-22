package com.universidad.rapientrega.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import com.universidad.rapientrega.models.Repartidor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepartidorRepository extends JpaRepository<Repartidor, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Repartidor r where r.disponible = true order by r.id")
    List<Repartidor> buscarDisponibles();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Repartidor r where r.id = :id")
    Optional<Repartidor> buscarConBloqueo(@Param("id") Long id);
}
