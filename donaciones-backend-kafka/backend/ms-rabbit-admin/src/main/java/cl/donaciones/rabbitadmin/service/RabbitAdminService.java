package cl.donaciones.rabbitadmin.service;

import cl.donaciones.rabbitadmin.dto.RabbitAdminDTOs.*;

/** Operaciones de administración de RabbitMQ (colas, exchanges y bindings). */
public interface RabbitAdminService {

    OperationResponse crearCola(QueueRequest request);

    QueueInfoResponse obtenerCola(String name);

    void eliminarCola(String name);

    OperationResponse crearExchange(ExchangeRequest request);

    void eliminarExchange(String name);

    OperationResponse crearBinding(BindingRequest request);

    void eliminarBinding(BindingRequest request);
}
