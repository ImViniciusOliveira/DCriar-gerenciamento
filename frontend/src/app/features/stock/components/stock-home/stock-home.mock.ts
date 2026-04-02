export interface StockHistoryProductRefMock {
  id: number;
  nome: string;
  sku: string;
}

export interface StockHistoryMovementMock {
  id: number;
  data: string;
  tipo: 'ENTRADA_PRODUCAO' | 'SAIDA_VENDA' | 'AJUSTE_MANUAL' | 'ENTRADA_ESTORNO' | 'ESTORNO_PRODUCAO';
  quantidade: number;
  motivo: string;
  produto: StockHistoryProductRefMock;
}

export interface StockHistoryResponseMock {
  _embedded: {
    movimentacoes: StockHistoryMovementMock[];
  };
  _links: {
    self: {
      href: string;
    };
    'ajustar-estoque-fisico': {
      href: string;
    };
    'por-produto-canais': {
      href: string;
    };
  };
}

export const STOCK_HISTORY_RESPONSE_MOCK: StockHistoryResponseMock = {
  _embedded: {
    movimentacoes: [
      {
        id: 2047,
        data: '2026-04-02T08:10:00',
        tipo: 'AJUSTE_MANUAL',
        quantidade: 24,
        motivo: 'Conferencia manual apos inventario rotativo do deposito principal',
        produto: {
          id: 11,
          nome: 'Adesivo Holografico 10x10cm',
          sku: 'ADH-10X10-HOLO'
        }
      },
      {
        id: 2041,
        data: '2026-04-02T07:42:00',
        tipo: 'AJUSTE_MANUAL',
        quantidade: 36,
        motivo: 'Diferenca positiva encontrada na contagem fisica',
        produto: {
          id: 11,
          nome: 'Adesivo Holografico 10x10cm',
          sku: 'ADH-10X10-HOLO'
        }
      },
      {
        id: 2038,
        data: '2026-04-02T07:11:00',
        tipo: 'SAIDA_VENDA',
        quantidade: -7,
        motivo: 'Venda #8841 sincronizada com o canal Mercado Livre',
        produto: {
          id: 7,
          nome: 'Banner Fosco 60x90cm',
          sku: 'BAN-60X90-FS'
        }
      },
      {
        id: 2034,
        data: '2026-04-02T06:27:00',
        tipo: 'ENTRADA_PRODUCAO',
        quantidade: 120,
        motivo: 'Entrada proveniente da Ordem de Producao #318',
        produto: {
          id: 4,
          nome: 'Cartao de Visita Couche 4x4',
          sku: 'CV-4X4-COUCHE'
        }
      },
      {
        id: 1980,
        data: '2026-03-18T14:05:00',
        tipo: 'ENTRADA_ESTORNO',
        quantidade: 18,
        motivo: 'Estorno integral da venda #8792',
        produto: {
          id: 13,
          nome: 'Papel Kraft 30x40cm',
          sku: 'PK-30X40'
        }
      },
      {
        id: 1871,
        data: '2025-12-11T09:14:00',
        tipo: 'ESTORNO_PRODUCAO',
        quantidade: -50,
        motivo: 'Reversao da Ordem de Producao #201 apos cancelamento',
        produto: {
          id: 22,
          nome: 'Etiqueta Fosca 5x3cm',
          sku: 'ETQ-5X3-FS'
        }
      }
    ]
  },
  _links: {
    self: {
      href: 'http://localhost:8080/api/v1/estoques/fisico/historico'
    },
    'ajustar-estoque-fisico': {
      href: 'http://localhost:8080/api/v1/estoques/ajuste-fisico'
    },
    'por-produto-canais': {
      href: 'http://localhost:8080/api/v1/estoques/por-produto-canais'
    }
  }
};
