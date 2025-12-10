/**
 * Representa um canal de distribuição e a quantidade de estoque associada a ele.
 */
export interface Channel {
  canalId: number;
  canalNome: string;
  quantidade: number;
}

/**
 * Representa o estoque de um produto distribuído por canais.
 * Utilizado para visualizar a disponibilidade do produto em diferentes frentes de venda.
 */
export interface ProductChannelStock {
  produtoId: number;
  produtoNome: string;
  canais: Channel[];
}
