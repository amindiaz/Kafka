import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/api.service';

/** Administración de colas, exchanges y bindings (ms-rabbit-admin, solo ADMIN). */
@Component({
  selector: 'app-rabbit-admin',
  imports: [FormsModule],
  templateUrl: './rabbit-admin.html',
})
export class RabbitAdmin {
  private api = inject(ApiService);

  mensaje = signal('');
  error = signal('');

  cola = { name: '', durable: true, deadLetterExchange: '', deadLetterRoutingKey: '' };
  exchange = { name: '', type: 'TOPIC' };
  binding = { queue: '', exchange: '', routingKey: '' };
  consultaCola = '';
  info = signal('');

  crearCola(): void {
    this.ejecutar(this.api.post('/api/rabbit/queues', this.cola), `Cola '${this.cola.name}' creada`);
  }

  eliminarCola(): void {
    this.ejecutar(this.api.delete(`/api/rabbit/queues/${this.cola.name}`), `Cola '${this.cola.name}' eliminada`);
  }

  verCola(): void {
    this.api.get<{ name: string; messageCount: number; consumerCount: number }>(`/api/rabbit/queues/${this.consultaCola}`)
      .subscribe({
        next: c => this.info.set(`${c.name}: ${c.messageCount} mensajes, ${c.consumerCount} consumidores`),
        error: e => this.info.set(this.mensajeDeError(e)),
      });
  }

  crearExchange(): void {
    this.ejecutar(this.api.post('/api/rabbit/exchanges', this.exchange), `Exchange '${this.exchange.name}' creado`);
  }

  eliminarExchange(): void {
    this.ejecutar(this.api.delete(`/api/rabbit/exchanges/${this.exchange.name}`), `Exchange '${this.exchange.name}' eliminado`);
  }

  crearBinding(): void {
    this.ejecutar(this.api.post('/api/rabbit/bindings', this.binding), 'Binding creado');
  }

  eliminarBinding(): void {
    const b = this.binding;
    const qs = `queue=${encodeURIComponent(b.queue)}&exchange=${encodeURIComponent(b.exchange)}&routingKey=${encodeURIComponent(b.routingKey)}`;
    this.ejecutar(this.api.delete(`/api/rabbit/bindings?${qs}`), 'Binding eliminado');
  }

  private ejecutar(op: Observable<unknown>, ok: string): void {
    this.mensaje.set('');
    this.error.set('');
    op.subscribe({
      next: () => this.mensaje.set(ok),
      error: e => this.error.set(this.mensajeDeError(e)),
    });
  }

  private mensajeDeError(e: HttpErrorResponse): string {
    return `Error ${e.status}: ${e.error?.message ?? e.error?.error ?? e.message}`;
  }
}
