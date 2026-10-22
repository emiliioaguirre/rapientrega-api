package com.universidad.rapientrega.services;

import com.universidad.rapientrega.models.CambioPedido;
import com.universidad.rapientrega.models.Calificacion;
import com.universidad.rapientrega.models.Cliente;
import com.universidad.rapientrega.models.DetallePedido;
import com.universidad.rapientrega.models.Pedido;
import com.universidad.rapientrega.models.Producto;
import com.universidad.rapientrega.models.Restaurante;
import com.universidad.rapientrega.repositories.CalificacionRepository;
import com.universidad.rapientrega.repositories.ClienteRepository;
import com.universidad.rapientrega.repositories.PedidoRepository;
import com.universidad.rapientrega.repositories.ProductoRepository;
import com.universidad.rapientrega.repositories.RestauranteRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import static org.springframework.http.HttpStatus.*;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final RestauranteRepository restaurantes;
    private final ProductoRepository productos;
    private final CalificacionRepository calificaciones;
    private final PagoService pagos;
    private final EntregaService entregas;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    public PedidoService(PedidoRepository pedidos, ClienteRepository clientes, RestauranteRepository restaurantes,
            ProductoRepository productos, CalificacionRepository calificaciones, PagoService pagos,
            EntregaService entregas, ApplicationEventPublisher eventos, Clock reloj) {
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.restaurantes = restaurantes;
        this.productos = productos;
        this.calificaciones = calificaciones;
        this.pagos = pagos;
        this.entregas = entregas;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    public List<Pedido> listar() {
        return pedidos.findAll();
    }

    public Pedido consultarPedido(Long id) {
        return pedidos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido no encontrado"));
    }

    @Transactional
    public Pedido registrarPedido(Pedido pedido) {
        if (pedido.getId() != null || pedido.getCliente() == null || pedido.getCliente().getId() == null
                || pedido.getRestaurante() == null || pedido.getRestaurante().getId() == null
                || pedido.getDetalles() == null || pedido.getDetalles().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requieren cliente, restaurante y productos, sin ID");
        }
        pedido.setCliente(clientes.findById(pedido.getCliente().getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Cliente no encontrado")));
        pedido.setRestaurante(restaurantes.findById(pedido.getRestaurante().getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Restaurante no encontrado")));
        BigDecimal total = BigDecimal.ZERO;
        for (DetallePedido detalle : pedido.getDetalles()) {
            if (detalle == null || detalle.getId() != null || detalle.getProducto() == null
                    || detalle.getProducto().getId() == null || detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                throw new ResponseStatusException(BAD_REQUEST, "Cada detalle requiere producto y cantidad positiva, sin ID");
            }
            Producto producto = productos.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Producto no encontrado"));
            if (!producto.isDisponible() || !producto.getRestaurante().getId().equals(pedido.getRestaurante().getId())) {
                throw new ResponseStatusException(BAD_REQUEST, "Producto no disponible en el restaurante elegido");
            }
            // usar siempre el precio registrado en el catálogo
            detalle.setProducto(producto);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            detalle.setPedido(pedido);
            total = total.add(detalle.getSubtotal());
        }
        pedido.setTotal(total);
        pedido.setEstado(Pedido.Estado.CREADO);
        pedido.setFechaCreacion(LocalDateTime.now(reloj));
        pedido.setLimiteAceptacion(null);
        pedido.setFechaEntrega(null);
        pedido.setFolioFactura(null);
        pedido.setLatitud(null);
        pedido.setLongitud(null);
        pedido.setRepartidor(null);
        pedido.setPago(null);
        Pedido guardado = pedidos.save(pedido);
        eventos.publishEvent(new CambioPedido(guardado.getId(), guardado.getEstado().name()));
        return guardado;
    }

    @Transactional
    public Pedido aceptarPedido(Long id) {
        Pedido pedido = buscarPendiente(id);
        if (!LocalDateTime.now(reloj).isBefore(pedido.getLimiteAceptacion())) {
            throw new ResponseStatusException(CONFLICT, "Venció el plazo de cinco minutos");
        }
        pedido.setEstado(Pedido.Estado.ACEPTADO);
        pedidos.save(pedido);
        return entregas.asignarRepartidor(id);
    }

    @Transactional
    public Pedido rechazarPedido(Long id) {
        Pedido pedido = buscarPendiente(id);
        cancelar(pedido);
        return pedido;
    }

    @Transactional
    public void cancelarVencido(Long id) {
        Pedido pedido = pedidos.buscarConBloqueo(id).orElse(null);
        if (pedido != null && pedido.getEstado() == Pedido.Estado.PENDIENTE_ACEPTACION
                && !LocalDateTime.now(reloj).isBefore(pedido.getLimiteAceptacion())) {
            cancelar(pedido);
        }
    }

    private Pedido buscarPendiente(Long id) {
        Pedido pedido = pedidos.buscarConBloqueo(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido no encontrado"));
        if (pedido.getEstado() != Pedido.Estado.PENDIENTE_ACEPTACION) {
            throw new ResponseStatusException(CONFLICT, "El pedido no está pendiente de aceptación");
        }
        return pedido;
    }

    private void cancelar(Pedido pedido) {
        pedido.setEstado(Pedido.Estado.CANCELADO);
        pedidos.save(pedido);
        pedido.setPago(pagos.reembolsarPago(pedido.getId()));
    }

    @Transactional
    public Calificacion calificarPedido(Long id, Calificacion calificacion) {
        Pedido pedido = pedidos.buscarConBloqueo(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido no encontrado"));
        if (pedido.getEstado() != Pedido.Estado.ENTREGADO || calificaciones.existsByPedidoId(id)) {
            throw new ResponseStatusException(CONFLICT, "Solo se califica una vez un pedido entregado");
        }
        if (calificacion.getId() != null || calificacion.getPuntaje() == null
                || calificacion.getPuntaje() < 1 || calificacion.getPuntaje() > 5
                || (calificacion.getComentario() != null && calificacion.getComentario().length() > 255)) {
            throw new ResponseStatusException(BAD_REQUEST, "Use un puntaje entre 1 y 5 y comentario de hasta 255 caracteres, sin ID");
        }
        calificacion.setPedido(pedido);
        return calificaciones.save(calificacion);
    }
}
