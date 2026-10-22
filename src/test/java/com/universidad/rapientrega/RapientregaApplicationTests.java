package com.universidad.rapientrega;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import com.universidad.rapientrega.models.Pedido;
import com.universidad.rapientrega.models.Repartidor;
import com.universidad.rapientrega.models.CambioPedido;
import com.universidad.rapientrega.repositories.PedidoRepository;
import com.universidad.rapientrega.services.PedidoService;
import com.universidad.rapientrega.services.PagoService;
import com.universidad.rapientrega.services.EntregaService;
import com.universidad.rapientrega.services.RepartidorService;
import com.universidad.rapientrega.services.SeguimientoService;
import com.universidad.rapientrega.controllers.PedidoController;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import jakarta.persistence.EntityManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=${TEST_DB_URL:jdbc:mysql://localhost:3306/rapientrega_pruebas}",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class RapientregaApplicationTests {
    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        String sesion = "pruebas-" + System.nanoTime();
        registro.add("pasarela.sesion", () -> sesion);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired RelojPrueba reloj;
    @Autowired PedidoRepository pedidos;
    @Autowired PedidoService servicio;
    @Autowired PagoService pagos;
    @Autowired EntregaService entregas;
    @Autowired RepartidorService repartidores;
    @Autowired PlatformTransactionManager transacciones;
    @Autowired EntityManager entidades;

    @TestConfiguration
    static class Configuracion {
        @Bean @Primary
        RelojPrueba relojPrueba() { return new RelojPrueba(); }
    }

    static class RelojPrueba extends Clock {
        private Instant ahora = Instant.parse("2026-10-23T18:00:00Z");
        public ZoneId getZone() { return ZoneId.of("America/Mexico_City"); }
        public Clock withZone(ZoneId zona) { return this; }
        public Instant instant() { return ahora; }
        void avanzar(long segundos) { ahora = ahora.plusSeconds(segundos); }
        void reiniciar() { ahora = Instant.parse("2026-10-23T18:00:00Z"); }
    }

    @BeforeEach
    void preparar() {
        reloj.reiniciar();
        for (String tabla : new String[]{"calificacion", "pago", "detalle_pedido", "pedido", "producto", "repartidor", "restaurante", "cliente"}) {
            jdbc.update("delete from " + tabla);
        }
        jdbc.update("insert into cliente (id,nombre,correo,direccion) values (10,'Emilio','202360079@ucc.mx','Veracruz')");
        jdbc.update("insert into restaurante (id,nombre,direccion,latitud,longitud) values (10,'Cocina Veracruz','Centro',19.17,-96.13),(11,'Otro restaurante','Norte',19.20,-96.15)");
        jdbc.update("insert into producto (id,nombre,precio,disponible,restaurante_id) values (10,'Torta',50,true,10),(11,'Agua',15,true,10),(12,'Pizza',90,true,11)");
        jdbc.update("insert into repartidor (id,nombre,latitud,longitud,disponible) values (10,'Cercano',19.171,-96.131,true),(11,'Lejano',19.21,-96.17,true)");
    }

    private Long pedido() throws Exception {
        mvc.perform(post("/api/v1/pedidos").contentType("application/json").content("""
                {"cliente":{"id":10},"restaurante":{"id":10},"total":1,
                 "detalles":[{"producto":{"id":10},"cantidad":2,"precioUnitario":1},
                             {"producto":{"id":11},"cantidad":1}]}
                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(115));
        return jdbc.queryForObject("select max(id) from pedido", Long.class);
    }

    private void pagar(Long id, String token) throws Exception {
        mvc.perform(post("/api/v1/pedidos/" + id + "/pagar").contentType("application/json")
                .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk());
    }

    private Long aceptado() throws Exception {
        Long id = pedido();
        pagar(id, "tok_demo_aprobado");
        mvc.perform(post("/api/v1/pedidos/" + id + "/aceptar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ASIGNADO"));
        return id;
    }

    private void esperarCancelacion(Long id) throws Exception {
        for (int i = 0; i < 80; i++) {
            String estado = jdbc.queryForObject("select estado from pedido where id = ?", String.class, id);
            if ("CANCELADO".equals(estado)) return;
            Thread.sleep(100);
        }
        fail("El scheduler no canceló el pedido vencido");
    }

    @Test
    void registraClienteParaIniciarUnPedido() throws Exception {
        mvc.perform(post("/api/v1/clientes").contentType("application/json").content("""
                {"nombre":"Emilio Alejandro Aguirre Tronco","correo":"202360079@ucc.mx","direccion":"Veracruz centro"}
                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void calculaTotalConPreciosDelServidor() throws Exception {
        Long id = pedido();
        mvc.perform(get("/api/v1/pedidos/" + id))
                .andExpect(jsonPath("$.total").value(115))
                .andExpect(jsonPath("$.detalles[0].precioUnitario").value(50));
    }

    @Test
    void rechazaProductosDeOtroRestaurante() throws Exception {
        mvc.perform(post("/api/v1/pedidos").contentType("application/json").content("""
                {"cliente":{"id":10},"restaurante":{"id":10},
                 "detalles":[{"producto":{"id":12},"cantidad":1}]}
                """))
                .andExpect(status().isBadRequest());
        assertEquals(0L, jdbc.queryForObject("select count(*) from pedido", Long.class));
    }

    @Test
    void rechazaCantidadesNoPositivas() throws Exception {
        mvc.perform(post("/api/v1/pedidos").contentType("application/json").content("""
                {"cliente":{"id":10},"restaurante":{"id":10},
                 "detalles":[{"producto":{"id":10},"cantidad":0}]}
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pagoAprobadoIniciaCincoMinutosYNoDuplicaCobro() throws Exception {
        Long id = pedido();
        pagar(id, "tok_demo_aprobado");
        pagar(id, "tok_demo_aprobado");
        mvc.perform(get("/api/v1/pedidos/" + id))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_ACEPTACION"))
                .andExpect(jsonPath("$.pago.estado").value("APROBADO"))
                .andExpect(jsonPath("$.limiteAceptacion").value("2026-10-23T12:05:00"));
        assertEquals(1L, jdbc.queryForObject("select count(*) from pago where pedido_id = ?", Long.class, id));
    }

    @Test
    void pagoRechazadoNoPasaAlRestaurante() throws Exception {
        Long id = pedido();
        pagar(id, "tok_demo_rechazado");
        mvc.perform(get("/api/v1/pedidos/" + id))
                .andExpect(jsonPath("$.estado").value("CREADO"))
                .andExpect(jsonPath("$.pago.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.limiteAceptacion").isEmpty());
        mvc.perform(post("/api/v1/pedidos/" + id + "/aceptar")).andExpect(status().isConflict());
    }

    @Test
    void falloHttpNoAfirmaUnCobro() throws Exception {
        Long id = pedido();
        mvc.perform(post("/api/v1/pedidos/" + id + "/pagar").contentType("application/json")
                .content("{\"token\":\"tok_demo_error\"}"))
                .andExpect(status().isBadGateway());
        mvc.perform(get("/api/v1/pedidos/" + id)).andExpect(jsonPath("$.estado").value("CREADO"));
        assertEquals(0L, jdbc.queryForObject("select count(*) from pago where pedido_id = ?", Long.class, id));
    }

    @Test
    void aceptarAntesDelLimiteReservaAlMasCercano() throws Exception {
        Long id = pedido();
        pagar(id, "tok_demo_aprobado");
        reloj.avanzar(299);
        mvc.perform(post("/api/v1/pedidos/" + id + "/aceptar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.repartidor.id").value(10))
                .andExpect(jsonPath("$.estado").value("ASIGNADO"));
        assertFalse(jdbc.queryForObject("select disponible from repartidor where id = 10", Boolean.class));
    }

    @Test
    void alVencerElPlazoSeCancelaYReembolsa() throws Exception {
        Long id = pedido();
        pagar(id, "tok_demo_aprobado");
        reloj.avanzar(300);
        mvc.perform(post("/api/v1/pedidos/" + id + "/aceptar")).andExpect(status().isConflict());
        esperarCancelacion(id);
        mvc.perform(get("/api/v1/pedidos/" + id))
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.pago.estado").value("REEMBOLSADO"));
    }

    @Test
    void rechazoDelRestauranteReembolsaSinDuplicar() throws Exception {
        Long id = pedido();
        pagar(id, "tok_demo_aprobado");
        mvc.perform(post("/api/v1/pedidos/" + id + "/rechazar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pago.estado").value("REEMBOLSADO"));
        mvc.perform(post("/api/v1/pedidos/" + id + "/rechazar")).andExpect(status().isConflict());
        assertEquals(1L, jdbc.queryForObject("select count(*) from pago where pedido_id = ?", Long.class, id));
    }

    @Test
    void reembolsoPendienteSeConservaYSeReintenta() throws Exception {
        Long id = pedido();
        pagar(id, "tok_demo_reembolso_pendiente");
        mvc.perform(post("/api/v1/pedidos/" + id + "/rechazar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.pago.estado").value("REEMBOLSO_PENDIENTE"));
        for (int i = 0; i < 80; i++) {
            String estado = jdbc.queryForObject("select estado from pago where pedido_id = ?", String.class, id);
            if ("REEMBOLSADO".equals(estado)) return;
            Thread.sleep(100);
        }
        fail("No se reintentó el reembolso pendiente");
    }

    @Test
    void sinRepartidorEsperaAsignacion() throws Exception {
        jdbc.update("update repartidor set disponible = false");
        Long id = pedido();
        pagar(id, "tok_demo_aprobado");
        mvc.perform(post("/api/v1/pedidos/" + id + "/aceptar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACEPTADO"))
                .andExpect(jsonPath("$.repartidor").isEmpty());
        jdbc.update("update repartidor set disponible = true where id = 10");
        mvc.perform(post("/api/v1/pedidos/" + id + "/asignar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.repartidor.id").value(10));
    }

    @Test
    void dosPedidosNoCompartenRepartidorReservado() throws Exception {
        jdbc.update("update repartidor set disponible = false where id = 11");
        Long primero = pedido();
        Long segundo = pedido();
        pagar(primero, "tok_demo_aprobado");pagar(segundo, "tok_demo_aprobado");
        CountDownLatch inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var uno = executor.submit(() -> { inicio.await();return mvc.perform(post("/api/v1/pedidos/" + primero + "/aceptar")).andReturn().getResponse().getStatus(); });
            var dos = executor.submit(() -> { inicio.await();return mvc.perform(post("/api/v1/pedidos/" + segundo + "/aceptar")).andReturn().getResponse().getStatus(); });
            inicio.countDown();
            assertEquals(200, uno.get(10, TimeUnit.SECONDS));assertEquals(200, dos.get(10, TimeUnit.SECONDS));
        }
        assertEquals(1L, jdbc.queryForObject("select count(*) from pedido where repartidor_id = 10", Long.class));
    }

    @Test
    void entregaLiberaRepartidorYGeneraFolio2012() throws Exception {
        Long id = aceptado();
        mvc.perform(post("/api/v1/pedidos/" + id + "/iniciar")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/pedidos/" + id + "/entregar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ENTREGADO"))
                .andExpect(jsonPath("$.folioFactura").value("FAC2012-" + id));
        assertTrue(jdbc.queryForObject("select disponible from repartidor where id = 10", Boolean.class));
    }

    @Test
    void noEntregaPedidoSinIniciarRecorrido() throws Exception {
        Long id = aceptado();
        mvc.perform(post("/api/v1/pedidos/" + id + "/entregar")).andExpect(status().isConflict());
    }

    @Test
    void calificacionSoloAlFinalYUnaVez() throws Exception {
        Long id = aceptado();
        String body = "{\"puntaje\":5,\"comentario\":\"Entrega puntual\"}";
        mvc.perform(post("/api/v1/pedidos/" + id + "/calificar").contentType("application/json").content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/pedidos/" + id + "/iniciar"));
        mvc.perform(post("/api/v1/pedidos/" + id + "/entregar"));
        mvc.perform(post("/api/v1/pedidos/" + id + "/calificar").contentType("application/json").content("{\"puntaje\":6}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/pedidos/" + id + "/calificar").contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.puntaje").value(5));
        mvc.perform(post("/api/v1/pedidos/" + id + "/calificar").contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void ubicacionNoRevierteUnaEntregaConfirmadaEnOtraTransaccion() throws Exception {
        Long id = aceptado();
        mvc.perform(post("/api/v1/pedidos/" + id + "/iniciar")).andExpect(status().isOk());
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            assertEquals(Pedido.Estado.EN_CAMINO,
                    entidades.createQuery("select p from Pedido p where p.repartidor.id = 10 and p.estado = :estado", Pedido.class)
                            .setParameter("estado", Pedido.Estado.EN_CAMINO).getResultList().getFirst().getEstado());
            CompletableFuture.runAsync(() -> entregas.entregarPedido(id)).join();
            Repartidor coordenadas = new Repartidor();
            coordenadas.setLatitud(19.19);
            coordenadas.setLongitud(-96.15);
            repartidores.actualizarUbicacion(10L, coordenadas);
        });
        assertEquals("ENTREGADO", jdbc.queryForObject("select estado from pedido where id = ?", String.class, id));
        assertEquals("FAC2012-" + id, jdbc.queryForObject("select folio_factura from pedido where id = ?", String.class, id));
        assertTrue(jdbc.queryForObject("select disponible from repartidor where id = 10", Boolean.class));
        assertEquals(19.171, jdbc.queryForObject("select latitud from pedido where id = ?", Double.class, id));
    }

    @Test
    void suscripcionNoPierdeEntregaEntreConsultaYRegistroDelEmisor() throws Exception {
        Long id = aceptado();
        mvc.perform(post("/api/v1/pedidos/" + id + "/iniciar")).andExpect(status().isOk());
        SeguimientoService seguimiento = new SeguimientoService(pedidos) {
            private boolean primerSnapshot = true;

            @Override
            public Map<String, Object> consultarSeguimiento(Long pedidoId) {
                Map<String, Object> datos = super.consultarSeguimiento(pedidoId);
                if (primerSnapshot) {
                    primerSnapshot = false;
                    entregas.entregarPedido(pedidoId);
                    alCambiarPedido(new CambioPedido(pedidoId, "ENTREGADO"));
                }
                return datos;
            }
        };
        MockMvc peticiones = MockMvcBuilders.standaloneSetup(
                new PedidoController(servicio, pagos, entregas, seguimiento)).build();
        MvcResult stream = peticiones.perform(get("/api/v1/pedidos/" + id + "/eventos"))
                .andExpect(request().asyncStarted()).andReturn();
        assertDoesNotThrow(() -> stream.getAsyncResult(200));
        peticiones.perform(asyncDispatch(stream)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ENTREGADO")));
    }

    @Test
    void seguimientoDePedidoTerminadoEnviaSnapshotYCierra() throws Exception {
        Long id = aceptado();
        mvc.perform(post("/api/v1/pedidos/" + id + "/iniciar"));
        mvc.perform(post("/api/v1/pedidos/" + id + "/entregar"));
        MvcResult stream = mvc.perform(get("/api/v1/pedidos/" + id + "/eventos"))
                .andExpect(request().asyncStarted()).andReturn();
        assertDoesNotThrow(() -> stream.getAsyncResult(200));
        mvc.perform(asyncDispatch(stream)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ENTREGADO")));
    }

    @Test
    void seguimientoSsePublicaCambiosConfirmados() throws Exception {
        Long id = aceptado();
        mvc.perform(get("/api/v1/pedidos/" + id + "/seguimiento"))
                .andExpect(jsonPath("$.repartidorId").value(10))
                .andExpect(jsonPath("$.latitud").value(19.171));
        MvcResult stream = mvc.perform(get("/api/v1/pedidos/" + id + "/eventos"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(post("/api/v1/pedidos/" + id + "/iniciar"));
        mvc.perform(put("/api/v1/repartidores/10/ubicacion").contentType("application/json")
                .content("{\"latitud\":19.18,\"longitud\":-96.14}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/pedidos/" + id + "/entregar"));
        mvc.perform(asyncDispatch(stream)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EN_CAMINO")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("19.18")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ENTREGADO")));
    }
}
