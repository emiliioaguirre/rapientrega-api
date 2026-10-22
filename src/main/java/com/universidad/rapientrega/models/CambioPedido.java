package com.universidad.rapientrega.models;

public class CambioPedido {
    private final Long idPedido;
    private final String estado;

    public CambioPedido(Long idPedido, String estado) {
        this.idPedido = idPedido;
        this.estado = estado;
    }

    public Long getIdPedido() {
        return idPedido;
    }
    public String getEstado() {
        return estado;
    }
}
