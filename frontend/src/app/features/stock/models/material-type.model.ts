import { Hateoas, PageInfo } from '../../../core/models/hateoas.model';
import { FieldLockMetadata } from '../../../shared/utils/field-locks';

/**
 * Representa a entidade Tipo de Matéria-Prima no sistema.
 * Mapeia a estrutura de dados retornada pela API, incluindo links HATEOAS para navegação.
 */
export interface MaterialType extends FieldLockMetadata {
  id: number;
  nome: string;
  unidadeDeConsumo: string;
  unidadeDescricao?: string;
  dataCriacao?: string;
  dataAtualizacao?: string;
  _links?: Hateoas['_links'];
}

/**
 * Objeto de transferência de dados (DTO) utilizado para criar ou atualizar um Tipo de Matéria-Prima.
 * Contém apenas os dados mutáveis necessários para a operação.
 */
export interface MaterialTypeRequest {
  nome: string;
  unidadeDeConsumo: string;
}

/**
 * Estrutura aninhada `_embedded` específica para a lista de Tipos de Matéria-Prima.
 */
export interface EmbeddedMaterialTypes {
  'tipos-materia-prima': MaterialType[];
}

/**
 * Estrutura de resposta padrão da API para listagens paginadas de Tipos de Matéria-Prima.
 * Contém os dados em `_embedded`, links de navegação e metadados de paginação.
 */
export interface ApiResponseMaterialTypes extends Hateoas {
  _embedded: EmbeddedMaterialTypes;
  page: PageInfo;
}
