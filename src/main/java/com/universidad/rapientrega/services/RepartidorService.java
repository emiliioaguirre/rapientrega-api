package com.universidad.rapientrega.services;

import com.universidad.rapientrega.models.CambioPedido;
import com.universidad.rapientrega.models.Pedido;
import com.universidad.rapientrega.models.Repartidor;
import com.universidad.rapientrega.repositories.PedidoRepository;
import com.universidad.rapientrega.repositories.RepartidorRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.ArrayList;
import static org.springframework.http.HttpStatus.*;

@Service
public class RepartidorService {
    private final RepartidorRepository repositorio;
    private final PedidoRepository pedidos;
    private final ApplicationEventPublisher eventos;
    private final EntityManager entidades;

    public RepartidorService(RepartidorRepository repositorio, PedidoRepository pedidos,
            ApplicationEventPublisher eventos, EntityManager entidades) {
        this.repositorio = repositorio;
        this.pedidos = pedidos;
        this.eventos = eventos;
        this.entidades = entidades;
    }

    public List<Repartidor> listar() {
        return repositorio.findAll();
    }

    public Repartidor registrarRepartidor(Repartidor repartidor) {
        if (repartidor.getId() != null || repartidor.getNombre() == null || repartidor.getNombre().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requiere el nombre, sin ID");
        }
        validarCoordenadas(repartidor.getLatitud(), repartidor.getLongitud());
        return repositorio.save(repartidor);
    }

    @Transactional
    public Repartidor actualizarUbicacion(Long id, Repartidor ubicacion) {
        validarCoordenadas(ubicacion.getLatitud(), ubicacion.getLongitud());
        List<Pedido> activos = new ArrayList<>();
        for (Long idPedido : pedidos.buscarIdsPorRepartidorYEstado(id, Pedido.Estado.EN_CAMINO)) {
            Pedido pedido = pedidos.buscarConBloqueo(idPedido).orElse(null);
            if (pedido != null) {
                // releer sin conservar una versión anterior del contexto
                entidades.detach(pedido);
                pedido = pedidos.buscarConBloqueo(idPedido).orElse(null);
            }
            if (pedido != null && pedido.getEstado() == Pedido.Estado.EN_CAMINO) activos.add(pedido);
        }
        Repartidor repartidor = repositorio.buscarConBloqueo(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Repartidor no encontrado"));
        entidades.detach(repartidor);
        repartidor = repositorio.buscarConBloqueo(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Repartidor no encontrado"));
        repartidor.setLatitud(ubicacion.getLatitud());
        repartidor.setLongitud(ubicacion.getLongitud());
        repositorio.save(repartidor);
        // avisar únicamente al reparto en camino
        for (Pedido pedido : activos) {
            pedido.setLatitud(ubicacion.getLatitud());
            pedido.setLongitud(ubicacion.getLongitud());
            pedidos.save(pedido);
            eventos.publishEvent(new CambioPedido(pedido.getId(), pedido.getEstado().name()));
        }
        return repartidor;
    }

    public static void validarCoordenadas(Double latitud, Double longitud) {
        if (latitud == null || longitud == null || !Double.isFinite(latitud) || !Double.isFinite(longitud)
                || latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requieren coordenadas válidas");
        }
    }
}
