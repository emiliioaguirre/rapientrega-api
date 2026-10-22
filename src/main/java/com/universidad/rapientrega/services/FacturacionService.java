package com.universidad.rapientrega.services;

import com.universidad.rapientrega.models.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FacturacionService {
    private static final Logger log = LoggerFactory.getLogger(FacturacionService.class);

    public String emitirFactura(Pedido pedido) {
        if (pedido.getFolioFactura() != null) {
            return pedido.getFolioFactura();
        }
        // adaptar los datos al formato del sistema de 2012
        String registro = pedido.getId() + ";" + pedido.getCliente().getNombre() + ";" + pedido.getTotal();
        String folio = "FAC2012-" + pedido.getId();
        log.info("[facturación 2012 simulada] registro={} folio={}", registro, folio);
        return folio;
    }
}
