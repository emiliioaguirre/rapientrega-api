package com.universidad.rapientrega.services;

import com.universidad.rapientrega.models.Producto;
import com.universidad.rapientrega.models.Restaurante;
import com.universidad.rapientrega.repositories.ProductoRepository;
import com.universidad.rapientrega.repositories.RestauranteRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.springframework.http.HttpStatus.*;

@Service
public class RestauranteService {
    private final RestauranteRepository restaurantes;
    private final ProductoRepository productos;

    public RestauranteService(RestauranteRepository restaurantes, ProductoRepository productos) {
        this.restaurantes = restaurantes;
        this.productos = productos;
    }

    public List<Restaurante> listar() {
        return restaurantes.findAll();
    }

    public Restaurante consultarRestaurante(Long id) {
        return restaurantes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Restaurante no encontrado"));
    }

    public Restaurante registrarRestaurante(Restaurante restaurante) {
        if (restaurante.getId() != null || restaurante.getNombre() == null || restaurante.getNombre().isBlank()
                || restaurante.getDireccion() == null || restaurante.getDireccion().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requieren nombre y dirección, sin ID");
        }
        RepartidorService.validarCoordenadas(restaurante.getLatitud(), restaurante.getLongitud());
        return restaurantes.save(restaurante);
    }

    public List<Producto> consultarProductos(Long id) {
        consultarRestaurante(id);
        return productos.findByRestauranteId(id);
    }

    public Producto registrarProducto(Long id, Producto producto) {
        if (producto.getId() != null || producto.getNombre() == null || producto.getNombre().isBlank()
                || producto.getPrecio() == null || producto.getPrecio().signum() <= 0
                || producto.getPrecio().stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requieren nombre y precio positivo de hasta dos decimales, sin ID");
        }
        producto.setRestaurante(consultarRestaurante(id));
        return productos.save(producto);
    }
}
