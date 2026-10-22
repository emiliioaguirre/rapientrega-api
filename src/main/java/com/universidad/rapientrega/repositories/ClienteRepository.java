package com.universidad.rapientrega.repositories;

import com.universidad.rapientrega.models.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
