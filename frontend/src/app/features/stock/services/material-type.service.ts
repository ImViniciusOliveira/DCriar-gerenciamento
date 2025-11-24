import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
// Import corrigido
import { ApiResponseMaterialTypes } from '../models/material-type.model';

@Injectable({
  providedIn: 'root',
})
export class MaterialTypeService {
  private readonly http = inject(HttpClient);

  searchMaterialTypes(
    baseUrl: string,
    filters: { nome?: string; unidadeDeConsumo?: string },
    page = 0,
    size = 20
    // Tipo de retorno corrigido
  ): Observable<ApiResponseMaterialTypes> {
    if (!baseUrl) {
      return throwError(() => new Error('URL de busca de matérias-primas não fornecida.'));
    }

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'nome,ASC');

    if (filters.nome?.trim()) {
      params = params.set('nome', filters.nome);
    }
    if (filters.unidadeDeConsumo?.trim()) {
      params = params.set('unidadeDeConsumo', filters.unidadeDeConsumo);
    }

    // Tipo de retorno corrigido
    return this.http.get<ApiResponseMaterialTypes>(baseUrl, { params });
  }
}
