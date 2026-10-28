package com.rica.investigadoresservice.investigadores.aplicacion;

import com.rica.investigadoresservice.investigadores.dominio.Investigador;

import java.util.List;
import java.util.Optional;

public interface RepositorioInvestigadores {

    List<Investigador> listarTodos();

    Optional<Investigador> buscarPorId(Long id);

    boolean existeCorreo(String correoInstitucional);

    Optional<Investigador> buscarPorCorreo(String correoInstitucional);

    Investigador guardar(Investigador investigador);

    void eliminar(Investigador investigador);
}
