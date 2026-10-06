package com.rica.investigadoresservice.investigadores.aplicacion;

import java.util.Map;

/**
 * Puerto de salida: la capa de aplicación pide "publicar un evento" sin saber
 * si por debajo hay una tabla outbox, Kafka o cualquier otra cosa.
 */
public interface PublicadorEventos {

    void publicar(String topico, String clave, Map<String, Object> contenido);

}
