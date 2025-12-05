import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponseTipoMateriaPrima, TipoMateriaPrima, TipoMateriaPrimaRequest } from '../models/tipo-materia-prima.model';

@Injectable({
  providedIn: 'root'
})
export class TipoMateriaPrimaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1/tipos-materia-prima';

  constructor() { }

  findAll(
    page: number,
    size: number,
    sort: string,
    order: string,
    nome?: string,
    unidadeDeConsumo?: string
  ): Observable<ApiResponseTipoMateriaPrima> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', `${sort},${order}`);

    if (nome) {
      params = params.set('nome', nome);
    }
    if (unidadeDeConsumo) {
      params = params.set('unidadeDeConsumo', unidadeDeConsumo);
    }

    return this.http.get<ApiResponseTipoMateriaPrima>(this.apiUrl, { params });
  }

  findById(id: number): Observable<TipoMateriaPrima> {
    return this.http.get<TipoMateriaPrima>(`${this.apiUrl}/${id}`);
  }

  create(request: TipoMateriaPrimaRequest): Observable<TipoMateriaPrima> {
    return this.http.post<TipoMateriaPrima>(this.apiUrl, request);
  }

  update(id: number, request: TipoMateriaPrimaRequest): Observable<TipoMateriaPrima> {
    return this.http.put<TipoMateriaPrima>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
