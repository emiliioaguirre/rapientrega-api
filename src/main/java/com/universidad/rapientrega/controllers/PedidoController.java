package com.universidad.rapientrega.controllers;

import com.universidad.rapientrega.models.Calificacion;
import com.universidad.rapientrega.models.Pago;
import com.universidad.rapientrega.models.Pedido;
import com.universidad.rapientrega.services.PedidoService;
import com.universidad.rapientrega.services.PagoService;
import com.universidad.rapientrega.services.EntregaService;
import com.universidad.rapientrega.services.SeguimientoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {
    private final PedidoService servicio;
    private final PagoService pagos;
    private final EntregaService entregas;
    private final SeguimientoService seguimiento;

    public PedidoController(PedidoService servicio, PagoService pagos, EntregaService entregas,
            SeguimientoService seguimiento) {
        this.servicio = servicio;
        this.pagos = pagos;
        this.entregas = entregas;
        this.seguimiento = seguimiento;
    }

    @GetMapping
    public List<Pedido> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public Pedido consultarPedido(@PathVariable Long id) {
        return servicio.consultarPedido(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pedido registrarPedido(@RequestBody Pedido pedido) {
        return servicio.registrarPedido(pedido);
    }

    @PostMapping("/{id}/pagar")
    public Pago procesarPago(@PathVariable Long id, @RequestBody Map<String, String> datos) {
        return pagos.procesarPago(id, datos.get("token"));
    }

    @PostMapping("/{id}/aceptar")
    public Pedido aceptarPedido(@PathVariable Long id) {
        return servicio.aceptarPedido(id);
    }

    @PostMapping("/{id}/rechazar")
    public Pedido rechazarPedido(@PathVariable Long id) {
        return servicio.rechazarPedido(id);
    }

    @PostMapping("/{id}/asignar")
    public Pedido asignarRepartidor(@PathVariable Long id) {
        return entregas.asignarRepartidor(id);
    }

    @PostMapping("/{id}/iniciar")
    public Pedido iniciarEntrega(@PathVariable Long id) {
        return entregas.iniciarEntrega(id);
    }

    @PostMapping("/{id}/entregar")
    public Pedido entregarPedido(@PathVariable Long id) {
        return entregas.entregarPedido(id);
    }

    @PostMapping("/{id}/calificar")
    @ResponseStatus(HttpStatus.CREATED)
    public Calificacion calificarPedido(@PathVariable Long id, @RequestBody Calificacion calificacion) {
        return servicio.calificarPedido(id, calificacion);
    }

    @GetMapping("/{id}/seguimiento")
    public Map<String, Object> consultarSeguimiento(@PathVariable Long id) {
        return seguimiento.consultarSeguimiento(id);
    }

    @GetMapping(value = "/{id}/eventos", produces = "text/event-stream")
    public SseEmitter suscribirPedido(@PathVariable Long id) {
        return seguimiento.suscribirPedido(id);
    }
}
