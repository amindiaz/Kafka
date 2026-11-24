import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';

interface Donacion {
  id: number;
  tipo: string;
  cantidad: number;
  unidad: string;
  origen: string;
  centroAcopio: string;
  estado: string;
}

@Component({
  selector: 'app-donaciones',
  imports: [FormsModule],
  templateUrl: './donaciones.html',
})
export class Donaciones implements OnInit {
  private api = inject(ApiService);
  auth = inject(AuthService);

  donaciones = signal<Donacion[]>([]);
  mensaje = signal('');
  error = signal('');

  nueva = { tipo: '', cantidad: 1, unidad: 'unidades', origen: '', centroAcopio: '' };
  readonly estados = ['Recibida', 'En_transito', 'Distribuida', 'Cancelada'];

  get puedeEscribir(): boolean {
    return this.auth.hasRole('ADMIN', 'OPERADOR');
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.api.get<Donacion[]>('/api/donaciones').subscribe({
      next: d => this.donaciones.set(d),
      error: e => this.error.set(`No se pudo cargar (${e.status})`),
    });
  }

  crear(): void {
    this.error.set('');
    this.api.post<Donacion>('/api/donaciones', this.nueva).subscribe({
      next: d => {
        this.mensaje.set(`Donación #${d.id} creada: se publicó el evento donacion.created en RabbitMQ`);
        this.nueva = { tipo: '', cantidad: 1, unidad: 'unidades', origen: '', centroAcopio: '' };
        this.cargar();
      },
      error: e => this.error.set(`No se pudo crear (${e.status})`),
    });
  }

  cambiarEstado(d: Donacion, estado: string): void {
    this.api.patch<Donacion>(`/api/donaciones/${d.id}/estado`, { estado }).subscribe({
      next: () => this.cargar(),
      error: e => this.error.set(`No se pudo cambiar el estado (${e.status})`),
    });
  }
}
