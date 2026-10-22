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
import java.time.Clock;
import java.time.LocalDateTime;
import static org.springframework.http.HttpStatus.*;

@Service
public class EntregaService {
    private final PedidoRepository pedidos;
    private final RepartidorRepository repartidores;
    private final FacturacionService facturacion;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    public EntregaService(PedidoRepository pedidos, RepartidorRepository repartidores,
            FacturacionService facturacion, ApplicationEventPublisher eventos, Clock reloj) {
        this.pedidos = pedidos;
        this.repartidores = repartidores;
        this.facturacion = facturacion;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Transactional
    public Pedido asignarRepartidor(Long id) {
        Pedido pedido = buscarPedido(id);
        if (pedido.getEstado() != Pedido.Estado.ACEPTADO) {
            throw new ResponseStatusException(CONFLICT, "Primero debe aceptarse el pedido");
        }
        Repartidor cercano = null;
        double menorDistancia = Double.MAX_VALUE;
        // reservar dentro de la misma transacción
        for (Repartidor repartidor : repartidores.buscarDisponibles()) {
            double distancia = distancia(pedido.getRestaurante().getLatitud(), pedido.getRestaurante().getLongitud(),
                    repartidor.getLatitud(), repartidor.getLongitud());
            if (distancia < menorDistancia) {
                cercano = repartidor;
                menorDistancia = distancia;
            }
        }
        if (cercano != null) {
            cercano.setDisponible(false);
            repartidores.save(cercano);
            pedido.setRepartidor(cercano);
            pedido.setLatitud(cercano.getLatitud());
            pedido.setLongitud(cercano.getLongitud());
            pedido.setEstado(Pedido.Estado.ASIGNADO);
        }
        pedidos.save(pedido);
        eventos.publishEvent(new CambioPedido(id, pedido.getEstado().name()));
        return pedido;
    }

    @Transactional
    public Pedido iniciarEntrega(Long id) {
        Pedido pedido = buscarPedido(id);
        if (pedido.getEstado() != Pedido.Estado.ASIGNADO) {
            throw new ResponseStatusException(CONFLICT, "El pedido debe tener un repartidor asignado");
        }
        pedido.setEstado(Pedido.Estado.EN_CAMINO);
        pedidos.save(pedido);
        eventos.publishEvent(new CambioPedido(id, pedido.getEstado().name()));
        return pedido;
    }

    @Transactional
    public Pedido entregarPedido(Long id) {
        Pedido pedido = buscarPedido(id);
        if (pedido.getEstado() != Pedido.Estado.EN_CAMINO) {
            throw new ResponseStatusException(CONFLICT, "El pedido debe estar en camino");
        }
        Repartidor repartidor = repartidores.buscarConBloqueo(pedido.getRepartidor().getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Repartidor no encontrado"));
        pedido.setEstado(Pedido.Estado.ENTREGADO);
        pedido.setFechaEntrega(LocalDateTime.now(reloj));
        pedido.setFolioFactura(facturacion.emitirFactura(pedido));
        repartidor.setDisponible(true);
        repartidores.save(repartidor);
        pedidos.save(pedido);
        eventos.publishEvent(new CambioPedido(id, pedido.getEstado().name()));
        return pedido;
    }

    private Pedido buscarPedido(Long id) {
        return pedidos.buscarConBloqueo(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido no encontrado"));
    }

    private double distancia(double lat1, double lon1, double lat2, double lon2) {
        double latitud = Math.toRadians(lat2 - lat1);
        double longitud = Math.toRadians(lon2 - lon1);
        double valor = Math.pow(Math.sin(latitud / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(longitud / 2), 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(valor), Math.sqrt(Math.max(0, 1 - valor)));
    }
}
