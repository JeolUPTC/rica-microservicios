package com.rica.investigadoresservice.investigadores.aplicacion;

import com.rica.investigadoresservice.compartido.RecursoNoEncontradoException;
import com.rica.investigadoresservice.investigadores.dominio.Investigador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class InvestigadorService implements InvestigadorUseCase {

    private final RepositorioInvestigadores repositorioInvestigadores;
    private final InvestigadorFactory investigadorFactory;

    private final PublicadorEventos publicadorEventos;

    public InvestigadorService(RepositorioInvestigadores repositorioInvestigadores,
                               InvestigadorFactory investigadorFactory,
                               PublicadorEventos publicadorEventos) {
        this.repositorioInvestigadores = repositorioInvestigadores;
        this.investigadorFactory = investigadorFactory;
        this.publicadorEventos = publicadorEventos;
    }

    @Override
    public List<Investigador> listarTodos() {
        return repositorioInvestigadores.listarTodos();
    }

    @Override
    public Investigador buscarPorId(Long id) {
        return repositorioInvestigadores.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un investigador con id " + id));
    }

    @Override
    @Transactional
    public Investigador registrar(String nombreCompleto, String correoInstitucional, String grupoInvestigacion) {
        Investigador investigador = investigadorFactory.crear(nombreCompleto, correoInstitucional, grupoInvestigacion);
        Investigador guardado = repositorioInvestigadores.guardar(investigador);

        // Transactional Outbox: el evento se guarda en la MISMA transacción que el investigador.
        publicadorEventos.publicar(
                "investigadores.registrados",
                correoInstitucional,
                Map.of("id", guardado.getId(),
                        "correoInstitucional", correoInstitucional,
                        "nombreCompleto", nombreCompleto));
        return guardado;
    }

}
