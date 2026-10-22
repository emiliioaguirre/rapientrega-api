package com.universidad.rapientrega.services;

import com.universidad.rapientrega.models.CambioPedido;
import com.universidad.rapientrega.models.Pago;
import com.universidad.rapientrega.models.Pedido;
import com.universidad.rapientrega.repositories.PagoRepository;
import com.universidad.rapientrega.repositories.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.LocalDateTime;
import static org.springframework.http.HttpStatus.*;

@Service
public class PagoService {
    private static final Logger log = LoggerFactory.getLogger(PagoService.class);
    private final PagoRepository pagos;
    private final PedidoRepository pedidos;
    private final PasarelaService pasarela;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    public PagoService(PagoRepository pagos, PedidoRepository pedidos, PasarelaService pasarela,
            ApplicationEventPublisher eventos, Clock reloj) {
        this.pagos = pagos;
        this.pedidos = pedidos;
        this.pasarela = pasarela;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Transactional
    public Pago procesarPago(Long id, String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requiere un token de tarjeta");
        }
        Pedido pedido = pedidos.buscarConBloqueo(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido no encontrado"));
        Pago anterior = pagos.findByPedidoId(id).orElse(null);
        if (anterior != null) {
            return anterior;
        }
        if (pedido.getEstado() != Pedido.Estado.CREADO) {
            throw new ResponseStatusException(CONFLICT, "El pedido no está disponible para pago");
        }
        PasarelaService.Respuesta respuesta = pasarela.cobrar(id, pedido.getTotal(), token);
        if (!"APROBADO".equals(respuesta.estado()) && !"RECHAZADO".equals(respuesta.estado())) {
            throw new ResponseStatusException(BAD_GATEWAY, "Estado de cobro no reconocido");
        }
        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMonto(pedido.getTotal());
        pago.setReferencia(respuesta.referencia());
        pago.setEstado(Pago.Estado.valueOf(respuesta.estado()));
        pago.setFecha(LocalDateTime.now(reloj));
        pagos.save(pago);
        pedido.setPago(pago);
        if (pago.getEstado() == Pago.Estado.APROBADO) {
            // el restaurante dispone de cinco minutos desde el cobro
            pedido.setEstado(Pedido.Estado.PENDIENTE_ACEPTACION);
            pedido.setLimiteAceptacion(LocalDateTime.now(reloj).plusMinutes(5));
            pedidos.save(pedido);
        }
        eventos.publishEvent(new CambioPedido(id, pedido.getEstado().name()));
        return pago;
    }

    @Transactional
    public Pago reembolsarPago(Long id) {
        Pedido pedido = pedidos.buscarConBloqueo(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido no encontrado"));
        Pago pago = pagos.findByPedidoId(id)
                .orElseThrow(() -> new ResponseStatusException(CONFLICT, "No existe un pago para reembolsar"));
        if (pago.getEstado() == Pago.Estado.REEMBOLSADO) {
            return pago;
        }
        if (pedido.getEstado() != Pedido.Estado.CANCELADO
                || (pago.getEstado() != Pago.Estado.APROBADO && pago.getEstado() != Pago.Estado.REEMBOLSO_PENDIENTE)) {
            throw new ResponseStatusException(CONFLICT, "El pago no puede reembolsarse");
        }
        // cancelar el pedido no equivale a confirmar el reembolso
        pago.setEstado(Pago.Estado.REEMBOLSO_PENDIENTE);
        try {
            PasarelaService.Respuesta respuesta = pasarela.reembolsar(id, pago.getReferencia(), pago.getMonto());
            if ("REEMBOLSADO".equals(respuesta.estado())) {
                pago.setEstado(Pago.Estado.REEMBOLSADO);
                pago.setReferenciaReembolso(respuesta.referencia());
            }
        } catch (ResponseStatusException error) {
            log.warn("[reembolso] pedido {} pendiente de confirmación", id);
        }
        pagos.save(pago);
        eventos.publishEvent(new CambioPedido(id, pedido.getEstado().name()));
        return pago;
    }
}
