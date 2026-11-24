import { Component, OnInit, inject, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';

interface Necesidad {
  id: number;
  recurso: string;
  cantidad: number;
  unidad: string;
  ubicacion: string;
  prioridad: string;
  estado: string;
}

@Component({
  selector: 'app-necesidades',
  templateUrl: './necesidades.html',
})
export class Necesidades implements OnInit {
  private api = inject(ApiService);

  necesidades = signal<Necesidad[]>([]);
  error = signal('');

  ngOnInit(): void {
    this.api.get<Necesidad[]>('/api/necesidades').subscribe({
      next: n => this.necesidades.set(n),
      error: e => this.error.set(`No se pudo cargar (${e.status})`),
    });
  }
}
