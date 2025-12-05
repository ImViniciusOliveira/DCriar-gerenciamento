import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class LoteMateriaPrimaService {
  findAll(_page = 0, _size = 10, _sort = 'id', _order = 'asc'): Observable<any> {
    const sample = {
      _embedded: {
        loteMateriaPrimaList: []
      },
      page: {
        totalElements: 0
      }
    };
    return of(sample);
  }
}
