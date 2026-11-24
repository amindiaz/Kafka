import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';

interface Envio {
  id: number;
  centroOrigenNombre: string;
  destino: string;
  descripcion: string;
  estado: string;
  donacionId: number | null;
}

/** Los envíos PLANIFICADOS se crean solos cuando ms-logistica consume donacion.created. */
@Component({
  selector: 'app-envios',
  templateUrl: './envios.html',
})
export class Envios implements OnInit {
  private api = inject(ApiService);

  envios = signal<Envio[]>([]);
  error = signal('');

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.api.get<Envio[]>('/api/logistica/envios').subscribe({
      next: e => this.envios.set(e),
      error: e => this.error.set(`No se pudo cargar (${e.status})`),
    });
  }
}
