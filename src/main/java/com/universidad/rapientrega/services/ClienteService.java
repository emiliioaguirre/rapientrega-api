package com.universidad.rapientrega.services;

import com.universidad.rapientrega.models.Cliente;
import com.universidad.rapientrega.repositories.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.springframework.http.HttpStatus.*;

@Service
public class ClienteService {
    private final ClienteRepository repositorio;

    public ClienteService(ClienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<Cliente> listar() {
        return repositorio.findAll();
    }

    public Cliente registrarCliente(Cliente cliente) {
        if (cliente.getId() != null || cliente.getNombre() == null || cliente.getNombre().isBlank()
                || cliente.getCorreo() == null || !cliente.getCorreo().contains("@")
                || cliente.getDireccion() == null || cliente.getDireccion().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Se requieren nombre, correo y dirección, sin ID");
        }
        return repositorio.save(cliente);
    }
}
