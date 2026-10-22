package com.universidad.rapientrega.controllers;

import com.universidad.rapientrega.models.Restaurante;
import com.universidad.rapientrega.models.Producto;
import com.universidad.rapientrega.services.RestauranteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurantes")
public class RestauranteController {
    private final RestauranteService servicio;

    public RestauranteController(RestauranteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Restaurante> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Restaurante consultarRestaurante(@PathVariable Long id) {
        return servicio.consultarRestaurante(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Restaurante registrarRestaurante(@RequestBody Restaurante restaurante) {
        return servicio.registrarRestaurante(restaurante);
    }

    @GetMapping("/{id}/productos")
    public List<Producto> consultarProductos(@PathVariable Long id) {
        return servicio.consultarProductos(id);
    }

    @PostMapping("/{id}/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public Producto registrarProducto(@PathVariable Long id, @RequestBody Producto producto) {
        return servicio.registrarProducto(id, producto);
    }
}
