import { MaterialType } from "../../stock/models/material-type.model";
import { UnidadeDeMedida } from "../../../../shared/models/unidade-de-medida.model";
import { Auditable } from "../../../../shared/models/auditable.model"; // Assumindo que você tem um modelo Auditable

export interface AtributosLote {
  [key: string]: any; // Ex: { "larguraMm": 600, "fornecedor": "Adesivos Premium" }
}

export interface LoteMateriaPrima extends Auditable { // Estende Auditable para dataCriacao e dataAtualizacao
  id: number;
  tipoMateriaPrima: MaterialType; // Objeto completo para exibição
  unidadeDeEstoque: UnidadeDeMedida;
  saldoEstoque: number; // BigDecimal no backend, number no frontend
  atributos: AtributosLote;
  loteDeOrigemId?: number; // Opcional
}

// DTO de requisição para criar/atualizar um lote
export interface LoteMateriaPrimaRequest {
  tipoMateriaPrimaId: number;
  unidadeDeEstoque: UnidadeDeMedida;
  quantidadeInicial: number; // BigDecimal no backend, number no frontend
  custoTotalLote: number; // BigDecimal no backend, number no frontend
  atributos?: AtributosLote;
  motivo?: string;
}
