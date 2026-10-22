package com.universidad.rapientrega.services;

import com.universidad.rapientrega.models.CambioPedido;
import com.universidad.rapientrega.models.Pedido;
import com.universidad.rapientrega.repositories.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SeguimientoService {
    private final PedidoRepository pedidos;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> suscriptores = new ConcurrentHashMap<>();

    public SeguimientoService(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }

    public Map<String, Object> consultarSeguimiento(Long id) {
        Pedido pedido = pedidos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido no encontrado"));
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("pedidoId", pedido.getId());
        datos.put("estado", pedido.getEstado());
        datos.put("repartidorId", pedido.getRepartidor() == null ? null : pedido.getRepartidor().getId());
        datos.put("latitud", pedido.getLatitud());
        datos.put("longitud", pedido.getLongitud());
        return datos;
    }

    public SseEmitter suscribirPedido(Long id) {
        consultarSeguimiento(id);
        SseEmitter emisor = new SseEmitter(300000L);
        CopyOnWriteArrayList<SseEmitter> lista = suscriptores.computeIfAbsent(id, clave -> new CopyOnWriteArrayList<>());
        lista.add(emisor);
        emisor.onCompletion(() -> lista.remove(emisor));
        emisor.onTimeout(emisor::complete);
        emisor.onError(error -> lista.remove(emisor));
        Map<String, Object> datos = consultarSeguimiento(id);
        enviar(emisor, datos);
        if (datos.get("estado") == Pedido.Estado.ENTREGADO || datos.get("estado") == Pedido.Estado.CANCELADO) {
            emisor.complete();
        }
        return emisor;
    }

    // publicar únicamente cambios confirmados en la base
    @TransactionalEventListener
    public void alCambiarPedido(CambioPedido cambio) {
        CopyOnWriteArrayList<SseEmitter> lista = suscriptores.get(cambio.getIdPedido());
        if (lista == null || lista.isEmpty()) return;
        Map<String, Object> datos = consultarSeguimiento(cambio.getIdPedido());
        for (SseEmitter emisor : lista) {
            enviar(emisor, datos);
            if ("ENTREGADO".equals(cambio.getEstado()) || "CANCELADO".equals(cambio.getEstado())) {
                emisor.complete();
            }
        }
    }

    private void enviar(SseEmitter emisor, Map<String, Object> datos) {
        try {
            emisor.send(SseEmitter.event().name("seguimiento").data(datos));
        } catch (IOException | IllegalStateException error) {
            emisor.completeWithError(error);
        }
    }
}
