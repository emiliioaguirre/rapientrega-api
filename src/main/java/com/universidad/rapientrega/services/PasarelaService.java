package com.universidad.rapientrega.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Service
public class PasarelaService {
    private final RestClient cliente;
    private final String sesion;

    public record Respuesta(String referencia, String estado) { }

    public PasarelaService(@Value("${pasarela.url}") String url,
            @Value("${pasarela.sesion:rapientrega}") String sesion) {
        this.sesion = sesion;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory peticiones = new JdkClientHttpRequestFactory(http);
        peticiones.setReadTimeout(Duration.ofSeconds(3));
        cliente = RestClient.builder().baseUrl(url).requestFactory(peticiones).build();
    }

    public Respuesta cobrar(Long id, BigDecimal monto, String token) {
        return enviar("/cobros", "pedido-" + id,
                Map.of("pedidoId", id, "monto", monto, "token", token));
    }

    public Respuesta reembolsar(Long id, String referencia, BigDecimal monto) {
        return enviar("/reembolsos", "reembolso-" + id,
                Map.of("referencia", referencia, "monto", monto));
    }

    private Respuesta enviar(String ruta, String clave, Map<String, Object> datos) {
        try {
            Respuesta respuesta = cliente.post().uri(ruta)
                    .header("Idempotency-Key", clave).header("x-mock-session", sesion)
                    .body(datos).retrieve().body(Respuesta.class);
            if (respuesta == null || respuesta.referencia() == null || respuesta.estado() == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "Respuesta incompleta de la pasarela");
            }
            return respuesta;
        } catch (RestClientException error) {
            throw new ResponseStatusException(BAD_GATEWAY, "No se confirmó la operación con la pasarela");
        }
    }
}
