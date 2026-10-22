package com.universidad.rapientrega.services;

import com.universidad.rapientrega.models.CambioPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class NotificacionService {
    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    @TransactionalEventListener
    public void alCambiarPedido(CambioPedido cambio) {
        log.info("[notificación simulada] pedido {} en estado {}", cambio.getIdPedido(), cambio.getEstado());
    }
}
