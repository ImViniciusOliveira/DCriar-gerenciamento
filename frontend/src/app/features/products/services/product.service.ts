import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, filter, map, switchMap, tap, shareReplay, take, catchError, of, combineLatest, finalize } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiRoot } from '../../../core/services/api-root';
import { Hateoas } from '../../../core/models/hateoas.model';
import { ApiResponseProducts, Product } from '../models/product.model';
import { Channel } from '../../stock/models/channel-stock.model';

/**
 * Serviço responsável pelo gerenciamento de Produtos.
 * Implementa uma arquitetura reativa e lida com a lógica de enriquecimento de dados de estoque,
 * além de ordenação e paginação no lado do cliente.
 */
@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly apiRoot = inject(ApiRoot);

  private readonly refreshTrigger = signal<void>(undefined, { equal: () => false });

  private readonly productSearchParams = signal<{ page: number; size: number; sort: string }>({
    page: 0,
    size: 10,
    sort: 'id,asc'
  }, {
    equal: (a, b) => a.page === b.page && a.size === b.size && a.sort === b.sort
  });

  /**
   * Signal que armazena os parâmetros para a busca especializada de produtos por estoque.
   * Esta busca é utilizada em contextos específicos como o formulário de produção.
   */
  private readonly productByStockSearchParams = signal<{
    tipoProduto: string | null;
    estoqueValor: number;
    estoqueOperador: 'GTE' | 'LTE';
    nome?: string;
  }>({
    tipoProduto: null,
    estoqueValor: 1,
    estoqueOperador: 'GTE',
    nome: ''
  }, {
    // Compara o objeto de parâmetros para evitar emissões desnecessárias se os filtros não mudaram.
    equal: (a, b) =>
      a.tipoProduto === b.tipoProduto &&
      a.estoqueValor === b.estoqueValor &&
      a.estoqueOperador === b.estoqueOperador &&
      a.nome === b.nome
  });

  /**
   * Sinal público que indica se a busca de produtos por estoque está em andamento.
   * Os componentes podem usar este sinal para exibir indicadores de carregamento.
   */
  readonly isSearchingByStock = signal(false);

  private readonly refresh$ = toObservable(this.refreshTrigger);
  private readonly productSearchParams$ = toObservable(this.productSearchParams);
  private readonly productByStockSearchParams$ = toObservable(this.productByStockSearchParams);

  /**
   * Observable reativo que emite a lista de Produtos.
   * É acionado sempre que os parâmetros de busca mudam ou um refresh manual é solicitado,
   * mantendo os componentes atualizados automaticamente.
   */
  readonly products$: Observable<ApiResponseProducts>;

  /**
   * Observable reativo para a busca especializada de produtos por estoque.
   * Emite uma lista de produtos sempre que os parâmetros em `productByStockSearchParams` são alterados.
   */
  readonly productsByStock$: Observable<Partial<Product>[]>;

  constructor() {
    const endpoints$ = toObservable(this.apiRoot.endpoints).pipe(shareReplay(1));

    this.products$ = endpoints$.pipe(
      switchMap(endpoints => this.createProductsObservable(endpoints))
    );

    this.productsByStock$ = endpoints$.pipe(
      switchMap(endpoints => this.createProductsByStockObservable(endpoints)),
      shareReplay(1)
    );
  }

  private createProductsObservable(endpoints: Hateoas | null): Observable<ApiResponseProducts> {
    if (!endpoints || !endpoints._links?.['produtos']?.href) {
      return of(this.createEmptyResponse());
    }

    return combineLatest([
      this.productSearchParams$,
      this.refresh$
    ]).pipe(
      switchMap(([params, _]) => {
        const productsRootUrl = endpoints._links?.['produtos']?.href;
        if (!productsRootUrl) {
           return of(this.createEmptyResponse());
        }

        const baseUrl = productsRootUrl.split('{')[0];
        const finalUrl = `${baseUrl}?page=${params.page}&size=${params.size}&sort=${params.sort}`;

        return this.http.get<any>(finalUrl).pipe(
          // Passo 1: Enriquecer os produtos com dados de estoque.
          switchMap(productsApiResponse => this.enrichProductsWithStock(productsApiResponse)),
          // Passo 2: Ordenar e paginar os dados combinados no lado do cliente.
          map(responseWithMergedStocks => this.sortAndPaginateClientSide(responseWithMergedStocks, params)),
          catchError(err => {
            console.error(`Falha ao buscar produtos na página ${params.page}, tamanho ${params.size}`, err);
            return of(this.createEmptyResponse());
          })
        );
      })
    );
  }

  /**
   * Constrói o fluxo de dados para a busca de produtos por estoque.
   * Este método é privado e chamado apenas no construtor para inicializar o `productsByStock$`.
   * @param endpoints O objeto de endpoints da API.
   * @returns Um Observable que emite a lista de produtos encontrados.
   */
  private createProductsByStockObservable(endpoints: Hateoas | null): Observable<Partial<Product>[]> {
    const productsUrl = endpoints?._links?.['produtos']?.href;
    if (!productsUrl) {
      console.error('Link "produtos" não encontrado na raiz da API.');
      return of([]);
    }
    const url = `${productsUrl.split('{')[0]}/by-tipo`;

    return this.productByStockSearchParams$.pipe(
      tap(() => this.isSearchingByStock.set(true)), // Ativa o spinner antes da busca
      switchMap(params => {
        let httpParams = new HttpParams()
          .set('estoqueValor', params.estoqueValor.toString())
          .set('estoqueOperador', params.estoqueOperador)
          .set('page', '0')
          .set('size', '100')
          .set('sort', 'nome,asc');

        if (params.tipoProduto) {
          httpParams = httpParams.set('tipoProduto', params.tipoProduto);
        }

        if (params.nome) {
          httpParams = httpParams.set('nome', params.nome);
        }

        return this.http.get<any>(url, { params: httpParams }).pipe(
          map(response => {
            const corte = response._embedded?.produtoDeCorteModelList || [];
            const consumo = response._embedded?.produtoDeConsumoDiretoModelList || [];
            return [...corte, ...consumo];
          }),
          catchError(err => {
            console.error('Erro ao buscar produtos por estoque:', err);
            return of([]);
          }),
          finalize(() => this.isSearchingByStock.set(false)) // Garante que o spinner seja desativado ao final
        );
      })
    );
  }

  private readonly endpoints$ = toObservable(this.apiRoot.endpoints).pipe(
    shareReplay(1)
  );

  /**
   * Retorna o fluxo observável principal de Produtos.
   */
  getProducts(): Observable<ApiResponseProducts> {
    return this.products$;
  }

  /**
   * Retorna o fluxo observável para a busca de produtos por estoque.
   * Os componentes devem se inscrever neste Observable para receber a lista de produtos.
   * A busca é acionada pela atualização dos parâmetros via `updateProductByStockSearchParams`.
   */
  getProductsByStock(): Observable<Partial<Product>[]> {
    return this.productsByStock$;
  }

  /**
   * Atualiza os parâmetros para a busca de produtos por estoque.
   * Chamar este método com novos parâmetros acionará uma nova emissão no `productsByStock$`.
   * @param params Um objeto parcial com os novos filtros a serem aplicados.
   */
  updateProductByStockSearchParams(params: Partial<{
    tipoProduto: string | null;
    estoqueValor: number;
    estoqueOperador: 'GTE' | 'LTE';
    nome?: string;
  }>): void {
    this.productByStockSearchParams.update(current => ({ ...current, ...params }));
  }

  /**
   * Atualiza os parâmetros de busca, o que dispara uma nova emissão no `products$`.
   */
  updateSearchParams(page: number, size: number, sort: string): void {
    this.productSearchParams.set({ page, size, sort });
  }

  /**
   * Busca produtos por nome ou SKU para uso em autocompletes.
   * Esta busca é stateless (não afeta a lista principal) e retorna uma lista simples de produtos.
   *
   * @param term Termo de busca (nome ou SKU)
   * @param channelId ID do canal de venda para filtrar estoque (opcional)
   * @param includeZeroStock Se true, inclui produtos com saldo zero na busca (padrão: false)
   */
  searchProducts(term: string, channelId?: number, includeZeroStock = false): Observable<Partial<Product>[]> {
    return this.endpoints$.pipe(
      take(1),
      switchMap(endpoints => {
        // Se tiver channelId, navega pela estrutura de links de estoque
        if (channelId) {
          const stocksRootUrl = endpoints._links?.['estoques']?.href;

          if (!stocksRootUrl) {
            console.error('[ProductService] Link "estoques" não encontrado na raiz da API.');
            return of([]);
          }

          // 1. Busca o recurso raiz de estoques para descobrir o link de resumo
          return this.http.get<Hateoas>(stocksRootUrl).pipe(
            switchMap(stocksRoot => {
              const resumoUrl = stocksRoot._links?.['resumo']?.href;
              if (!resumoUrl) {
                console.error('[ProductService] Link "resumo" não encontrado no recurso de estoques.');
                return of([]);
              }

              // Limpeza agressiva da URL para remover templates e query params existentes
              const url = resumoUrl.split('?')[0].split('{')[0];

              const params = new HttpParams()
                .set('canalId', channelId.toString())
                .set('nomeProduto', term)
                .set('apenasComSaldo', (!includeZeroStock).toString()) // Inverte a lógica: se incluir zero, apenasComSaldo = false
                .set('page', '0')
                .set('size', '10');

              // 2. Faz a busca no endpoint descoberto
              return this.http.get<any>(url, { params }).pipe(
                map(response => {
                  return (response._embedded?.estoqueProdutoResumoDTOList || []).map((dto: any) => ({
                    id: dto.produtoId,
                    nome: dto.nomeProduto,
                    sku: dto.skuProduto,
                    estoquePorCanal: { [channelId]: dto.quantidadeNoCanal },
                    _tempPrice: dto.precoVenda,
                    estoqueDisponivel: dto.quantidadeNoCanal
                  } as Partial<Product>));
                })
              );
            }),
            catchError(err => {
              console.error('[ProductService] Erro na navegação/busca de estoque:', err);
              return of([]);
            })
          );
        }

        // Fallback para a busca antiga se não tiver channelId
        const url = endpoints?._links?.['produtos']?.href;
        if (!url) return of([]);

        const baseUrl = url.split('{')[0];
        const params = new HttpParams()
          .set('page', '0')
          .set('size', '10')
          .set('nome', term);

        return this.http.get<any>(baseUrl, { params }).pipe(
          map(response => {
            const corte = response._embedded?.produtoDeCorteModelList || [];
            const consumo = response._embedded?.produtoDeConsumoDiretoModelList || [];
            return [...corte, ...consumo];
          }),
          switchMap(products => {
             if (products.length === 0) return of([]);
             const productIds = products.map((p: Product) => p.id);
             const stockUrl = endpoints._links?.['estoques-por-produtos']?.href;
             if (!stockUrl) return of(products);

             return this.getStocksForProducts(productIds, stockUrl).pipe(
               map(allStocks => {
                 return products.map((product: Product) => ({
                   ...product,
                   estoquePorCanal: allStocks[product.id] || {},
                 }));
               }),
               catchError(() => of(products))
             );
          }),
          catchError(err => {
            console.error('Erro na busca de produtos:', err);
            return of([]);
          })
        );
      })
    );
  }

  /**
   * Busca um Produto específico pela sua URL completa.
   */
  getProductByUrl(url: string): Observable<Product> {
    return this.http.get<Product>(url);
  }

  /**
   * Retorna um template HATEOAS para a criação de um novo Produto.
   */
  getNewProductTemplate(): Observable<Product> {
    return this.endpoints$.pipe(
      filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
      map(endpoints => this.getProductBaseUrl(endpoints)),
      switchMap(baseUrl => this.http.get<Product>(`${baseUrl}/new`)),
      take(1)
    );
  }

  /**
   * Remove um Produto pela sua URL.
   */
  deleteProduct(url: string): Observable<void> {
    return this.http.delete<void>(url).pipe(
      tap(() => this.refreshTrigger.set(undefined))
    );
  }

  /**
   * Cria um novo Produto na API.
   * @param product O payload parcial para a criação.
   * @param skipRefresh Se true, não dispara a atualização da lista.
   */
  createProduct(product: Partial<Product>, skipRefresh = false): Observable<Product> {
    const payload = this.mapToPayload(product);
    return this.endpoints$.pipe(
      filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
      take(1),
      map(endpoints => this.getProductBaseUrl(endpoints)),
      switchMap(url => this.http.post<Product>(url, payload)),
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); })
    );
  }

  /**
   * Atualiza parcialmente um Produto existente na API.
   * @param productId O ID do produto a ser atualizado.
   * @param product O payload com as alterações.
   * @param skipRefresh Se true, não dispara a atualização da lista.
   */
  patchProduct(productId: number, product: Partial<Product>, skipRefresh = false): Observable<Product> {
    delete product.id;
    const payload = this.mapToPayload(product);

    return this.endpoints$.pipe(
      filter((endpoints): endpoints is NonNullable<typeof endpoints> => !!endpoints),
      map(endpoints => this.getProductBaseUrl(endpoints)),
      switchMap(baseUrl => this.http.patch<Product>(`${baseUrl}/${productId}`, payload)),
      tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); }),
      take(1)
    );
  }

  /**
   * Realiza o upload da foto de um produto.
   * @param uploadUrl A URL específica para o upload da foto.
   * @param file O arquivo de imagem.
   * @param skipRefresh Se true, não dispara a atualização da lista.
   */
  uploadProductPhoto(uploadUrl: string, file: File, skipRefresh = false): Observable<Product> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<Product>(uploadUrl, formData)
      .pipe(tap(() => { if (!skipRefresh) this.refreshTrigger.set(undefined); }));
  }

  /**
   * Busca os estoques para uma lista de IDs de produtos em um endpoint otimizado.
   */
  getStocksForProducts(productIds: number[], stockUrl: string): Observable<{ [productId: string]: { [channelKey: string]: number } }> {
    if (productIds.length === 0) {
      return of({});
    }

    const baseUrl = stockUrl.split('{')[0];
    const url = new URL(baseUrl);
    url.searchParams.set('produtoIds', productIds.join(','));

    return this.http.get<any>(url.toString()).pipe(
      map(response => {
        const stockMap: { [productId: string]: { [channelKey: string]: number } } = {};
        const productStocks = response?._embedded?.produtoEstoqueResponseDTOList || [];
        for (const item of productStocks) {
          stockMap[item.produtoId] = this.createChannelMap(item.canais);
        }
        return stockMap;
      })
    );
  }

  /**
   * Combina a resposta da API de produtos com os dados de estoque de canais.
   */
  private enrichProductsWithStock(productsApiResponse: any): Observable<ApiResponseProducts> {
    const productsFromApi = [
      ...(productsApiResponse?._embedded?.produtoDeCorteModelList || []),
      ...(productsApiResponse?._embedded?.produtoDeConsumoDiretoModelList || [])
    ];
    const stockUrl = productsApiResponse?._links?.['estoques-por-produtos']?.href;

    if (productsFromApi.length === 0 || !stockUrl) {
      return of({
        ...productsApiResponse,
        _embedded: { produtos: productsFromApi }
      });
    }

    const productIds = productsFromApi.map((p: Product) => p.id);
    return this.getStocksForProducts(productIds, stockUrl).pipe(
      map(allStocks => {
        const mergedProducts = productsFromApi.map((product: Product) => ({
          ...product,
          estoquePorCanal: allStocks[product.id] || {},
        }));
        return {
          ...productsApiResponse,
          _embedded: { produtos: mergedProducts }
        };
      }),
      catchError(() => {
        console.warn('Falha ao buscar estoque para produtos, retornando produtos sem estoque por canal.');
        return of({
          ...productsApiResponse,
          _embedded: { produtos: productsFromApi }
        });
      })
    );
  }

  /**
   * Realiza a ordenação e paginação dos produtos no lado do cliente.
   * Nota: A paginação é feita no cliente para este serviço específico devido à necessidade
   * de consolidar o estoque antes de exibir os dados.
   */
  private sortAndPaginateClientSide(response: ApiResponseProducts, params: { size: number, sort: string }): ApiResponseProducts {
    const products = response._embedded.produtos;
    const [sortField, sortOrder] = params.sort.split(',');

    products.sort((a: any, b: any) => {
      const getNestedValue = (obj: any, path: string) => path.split('.').reduce((o, key) => o && o[key], obj);
      const valueA = getNestedValue(a, sortField);
      const valueB = getNestedValue(b, sortField);

      if (valueA == null) return 1;
      if (valueB == null) return -1;

      if (valueA < valueB) return sortOrder === 'asc' ? -1 : 1;
      if (valueA > valueB) return sortOrder === 'asc' ? 1 : -1;
      return 0;
    });

    // Preserva o `totalElements` original da API para que o paginador funcione corretamente.
    const totalElementsFromApi = response.page?.totalElements ?? products.length;

    return {
      _embedded: { produtos: products },
      _links: response._links,
      page: {
        size: params.size,
        totalElements: totalElementsFromApi,
        totalPages: Math.ceil(totalElementsFromApi / params.size),
        number: response.page?.number ?? 0
      }
    } as ApiResponseProducts;
  }

  private mapToPayload(product: Partial<Product>): any {
    const payload: any = { ...product };
    // Mapeia o objeto de matéria-prima para o ID esperado pelo backend.
    if (payload.materiaPrima) {
      payload.tipoMateriaPrimaId = payload.materiaPrima.id;
      delete payload.materiaPrima;
    }
    return payload;
  }

  private createChannelMap(channels: Channel[]): { [key: string]: number } {
    return Object.fromEntries(channels.map(channel => [channel.canalNome, channel.quantidade]));
  }

  private getProductBaseUrl(endpoints: Hateoas): string {
    const url = endpoints?._links?.['produtos']?.href;
    if (!url) {
      throw new Error('URL de produtos não encontrada na resposta da API');
    }
    return url.split('{')[0];
  }

  private createEmptyResponse(): ApiResponseProducts {
    return { _embedded: { produtos: [] }, _links: {}, page: { size: 0, totalElements: 0, totalPages: 0, number: 0 } };
  }
}
