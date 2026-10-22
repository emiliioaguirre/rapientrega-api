package com.universidad.rapientrega.services;

import com.universidad.rapientrega.models.Pago;
import com.universidad.rapientrega.models.Pedido;
import com.universidad.rapientrega.repositories.PagoRepository;
import com.universidad.rapientrega.repositories.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class PedidoScheduler {
    private static final Logger log = LoggerFactory.getLogger(PedidoScheduler.class);
    private final PedidoRepository pedidos;
    private final PagoRepository pagos;
    private final PedidoService servicio;
    private final PagoService cobros;
    private final Clock reloj;

    public PedidoScheduler(PedidoRepository pedidos, PagoRepository pagos, PedidoService servicio,
            PagoService cobros, Clock reloj) {
        this.pedidos = pedidos;
        this.pagos = pagos;
        this.servicio = servicio;
        this.cobros = cobros;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void revisarPedidos() {
        for (Pedido pedido : pedidos.findByEstadoAndLimiteAceptacionLessThanEqual(
                Pedido.Estado.PENDIENTE_ACEPTACION, LocalDateTime.now(reloj))) {
            try {
                servicio.cancelarVencido(pedido.getId());
            } catch (RuntimeException error) {
                log.error("[pedido] no se pudo cancelar {}: {}", pedido.getId(), error.getMessage());
            }
        }
        for (Pago pago : pagos.findByEstado(Pago.Estado.REEMBOLSO_PENDIENTE)) {
            try {
                cobros.reembolsarPago(pago.getPedido().getId());
            } catch (RuntimeException error) {
                log.error("[reembolso] sigue pendiente el pago {}: {}", pago.getId(), error.getMessage());
            }
        }
    }
}
