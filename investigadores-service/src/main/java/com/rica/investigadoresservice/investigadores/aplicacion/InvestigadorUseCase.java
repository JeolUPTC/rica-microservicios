package com.rica.investigadoresservice.investigadores.aplicacion;

import com.rica.investigadoresservice.investigadores.dominio.Investigador;

import java.util.List;

public interface InvestigadorUseCase {

    List<Investigador> listarTodos();

    Investigador buscarPorId(Long id);

    Investigador registrar(String nombreCompleto, String correoInstitucional, String grupoInvestigacion);

    /** Paso 1 de la Saga: marca al investigador como EN_ELIMINACION y publica el evento. */
    void iniciarEliminacion(Long id);

    /** Paso final de la Saga: borrado físico, una vez publicaciones confirmó. */
    void confirmarEliminacion(String correoInstitucional);
}
