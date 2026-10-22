package com.universidad.rapientrega.controllers;

import com.universidad.rapientrega.models.Repartidor;
import com.universidad.rapientrega.services.RepartidorService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.List;

@RestController
@RequestMapping("/api/v1/repartidores")
public class RepartidorController {
    private final RepartidorService servicio;

    public RepartidorController(RepartidorService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Repartidor> listar() {
        return servicio.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Repartidor registrarRepartidor(@RequestBody Repartidor repartidor) {
        return servicio.registrarRepartidor(repartidor);
    }

    @PutMapping("/{id}/ubicacion")
    public Repartidor actualizarUbicacion(@PathVariable Long id, @RequestBody Repartidor ubicacion) {
        return servicio.actualizarUbicacion(id, ubicacion);
    }
}
