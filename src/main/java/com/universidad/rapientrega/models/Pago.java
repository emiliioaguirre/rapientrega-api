package com.universidad.rapientrega.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToOne;

@Entity
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum Estado { APROBADO, RECHAZADO, REEMBOLSO_PENDIENTE, REEMBOLSADO }

    @OneToOne
    @JoinColumn(nullable = false, unique = true)
    @JsonIgnore
    private Pedido pedido;

    @Column(nullable = false)
    private BigDecimal monto;

    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    private String referenciaReembolso;

    private LocalDateTime fecha;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public String getReferenciaReembolso() {
        return referenciaReembolso;
    }

    public void setReferenciaReembolso(String referenciaReembolso) {
        this.referenciaReembolso = referenciaReembolso;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
