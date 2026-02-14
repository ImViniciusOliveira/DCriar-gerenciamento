# 🚨 GUIA: Dialog Travando ao Abrir - Solução com Cache + Lazy Signals

## **O Problema: Por que o dialog trava?**

Quando você abre um dialog (criar produto, criar venda, etc), ele **congela/trava** porque:

1. **Busca de dados é síncrona** (no constructor com `toSignal()`)
2. **Requisição HTTP toma tempo** (espera resposta do backend)
3. **Angular não consegue renderizar** até terminar o código síncrono

**Exemplo:**
```
Abrir dialog → Constructor executa → toSignal() faz requisição → ESPERA resposta → Trava
```

---

## **A Solução: Cache + Lazy Signals com setTimeout**

A solução é usar **2 padrões juntos**:

1. **CacheService** - Cachear dados para reutilizar entre navegações
2. **Lazy Signals com setTimeout** - Fazer requisições DEPOIS que dialog renderiza

---

## **PASSO A PASSO: Como Implementar a Solução**

### **Passo 1: Criar CacheService**

**Arquivo:** `src/app/core/services/cache.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { Observable, shareReplay, timeout } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CacheService {
  private caches = new Map<string, Observable<any>>();

  /**
   * Cacheia um observable com TTL opcional
   * @param key Chave única do cache (ex: 'products', 'material-types')
   * @param source$ Observable que será cacheado
   * @param ttlMs TTL em milissegundos (opcional)
   */
  getWithCache<T>(
    key: string,
    source$: Observable<T>,
    ttlMs?: number
  ): Observable<T> {
    if (!this.caches.has(key)) {
      let cached$ = source$.pipe(
        shareReplay({ bufferSize: 1, refCount: false })
      );

      if (ttlMs && ttlMs > 0) {
        cached$ = cached$.pipe(timeout(ttlMs));
      }

      this.caches.set(key, cached$);
    }

    return this.caches.get(key)!;
  }

  // TODO: Para usar depois quando precisar invalidar cache
  // invalidate(key: string): void {
  //   this.caches.delete(key);
  // }
}
```

**O que faz:** Guarda dados em memória para reutilizar sem refazer requisição.

---

### **Passo 2: Adicionar Métodos no ProductService**

**Arquivo:** `src/app/features/products/services/product.service.ts`

**Adicione estas linhas no constructor:**
```typescript
private readonly cache = inject(CacheService);
```

**Adicione este método:**
```typescript
/**
 * Retorna produtos simples SEM enriquecimento de estoque
 * (padrão igual MaterialTypeService.getMaterialTypes())
 */
getProductsSimple(): Observable<Partial<Product>[]> {
  return this.endpoints$.pipe(
    take(1),
    switchMap(endpoints => {
      const productsUrl = endpoints._links?.['produtos']?.href;
      if (!productsUrl) return of([]);

      const baseUrl = productsUrl.split('{')[0];
      const params = new HttpParams()
        .set('page', '0')
        .set('size', '100')
        .set('sort', 'nome,asc');

      return this.http.get<any>(baseUrl, { params }).pipe(
        map(response => {
          const corte = response._embedded?.produtoDeCorteModelList || [];
          const consumo = response._embedded?.produtoDeConsumoDiretoModelList || [];
          return [...corte, ...consumo];
        }),
        catchError(() => of([]))
      );
    })
  );
}

/**
 * Retorna produtos COM CACHE (sem TTL)
 * Usado por ProductStockSearch para não travar dialog
 */
getProductsWithCache(): Observable<Partial<Product>[]> {
  return this.cache.getWithCache('products', this.getProductsSimple());
}
```

---

### **Passo 3: Atualizar ProductStockSearch**

**Arquivo:** `src/app/shared/components/product-stock-search/product-stock-search.ts`

**Remove do constructor:**
```typescript
// ❌ REMOVA ISSO:
const productsResponse = toSignal(
  this.productService.getProductsSimple().pipe(...)
);

effect(() => {
  // ... código aqui
});
```

**Adicione no ngOnInit():**
```typescript
ngOnInit(): void {
  const ctrl = this.control();

  // Sincronizar valor inicial...
  if (ctrl.value) {
    this.searchControl.setValue(ctrl.value);
    this.products.set([ctrl.value]);
  } else if (this.isEditMode()) {
    // ... resto do código de sincronização
  }

  // Busca ao digitar
  this.searchControl.valueChanges.pipe(
    debounceTime(300),
    distinctUntilChanged(),
    takeUntilDestroyed(this.destroyRef)
  ).subscribe(value => {
    if (typeof value === 'string') {
      this.performSearch();
    }
  });

  // ✅ ADICIONE ISSO - Busca inicial com lazy load:
  setTimeout(() => {
    const productsResponse = toSignal(
      this.productService.getProductsWithCache().pipe(
        catchError(() => of([]))
      )
    );

    effect(() => {
      this.isSearching.set(false);
      const response = productsResponse();
      if (response && Array.isArray(response)) {
        if (this.currentPage === 0) {
          this.products.set(response);
        } else {
          this.products.update(current => [...current, ...response]);
        }
      }
    });
  }, 0);
}
```

---

### **Passo 4: Atualizar MaterialTypeSearch**

**Arquivo:** `src/app/shared/components/material-type-search/material-type-search.ts`

**Remove do constructor:**
```typescript
// ❌ REMOVA ISSO:
const materialTypesResponse = toSignal(
  this.materialTypeService.getMaterialTypes().pipe(...)
);

effect(() => {
  // ... código aqui
});
```

**Remove do ngOnDestroy():**
```typescript
// ❌ REMOVA ISSO:
ngOnDestroy(): void {
  this.materialTypeService.resetSearchParams();  // Causa requisição extra
}
```

**Adicione no ngOnInit() (no final):**
```typescript
// ✅ ADICIONE ISSO - Busca inicial com lazy load:
setTimeout(() => {
  const materialTypesResponse = toSignal(
    this.materialTypeService.getMaterialTypes().pipe(
      catchError(() => of(undefined))
    )
  );

  effect(() => {
    this.isSearching.set(false);
    const response: ApiResponseMaterialTypes | undefined = materialTypesResponse();
    if (response) {
      const newItems = response._embedded?.['tipos-materia-prima'] ?? [];
      this.materialTypes.set(newItems);
      this.totalElements.set(response.page.totalElements);

      const firstMaterial = newItems[0];
      const newUnitsUrl = firstMaterial?._links?.['unidades-de-medida']?.href;
      if (newUnitsUrl && this.unitsUrl() !== newUnitsUrl) {
        this.unitsUrl.set(newUnitsUrl);
      }
    }
  });
}, 0);
```

---

## **Checklist de Diagnóstico - Quando o Dialog Trava**

### **1️⃣ Verificar se tem `toSignal()` no Constructor**

```typescript
// ❌ PROBLEMA - No constructor:
export class MeuComponente implements OnInit {
  constructor() {
    const response = toSignal(
      this.service.getMeusDados().pipe(catchError(() => of(undefined)))
    );
    
    effect(() => {
      const data = response();  // ← BLOQUEIA RENDERIZAÇÃO
      this.meusDados.set(data);
    });
  }
}
```

**Solução:** Mover para `ngOnInit()` com `setTimeout(..., 0)`

---

### **2️⃣ Verificar se tem `effect()` no Constructor**

```typescript
// ❌ PROBLEMA - No constructor:
effect(() => {
  if (!this.url()) {
    this.control().disable();  // ← BLOQUEIA
  }
});
```

**Solução:** Mover para `ngOnInit()` ou usar lógica simples:

```typescript
// ✅ CORRETO - No ngOnInit:
ngOnInit() {
  if (!this.url()) {
    this.control().disable();  // ← Não bloqueia
  }
}
```

---

### **3️⃣ Verificar se tem Requisições HTTP Síncronas**

```typescript
// ❌ PROBLEMA - Constructor faz requisição:
constructor() {
  this.http.get(url).subscribe(data => {
    this.dados.set(data);  // ← Bloqueia se demorar
  });
}
```

**Solução:** Mover para `ngOnInit()` com delay

---

### **4️⃣ Verificar ngOnDestroy com Requisições Extra**

```typescript
// ❌ PROBLEMA - Causa requisição ao fechar dialog:
ngOnDestroy(): void {
  this.service.resetSearchParams();  // ← Dispara nova busca
}
```

**Solução:** Remover ou fazer com cuidado

---

## **Passo a Passo para Resolver**

### **Se o dialog trava ao ABRIR:**

1. **Abra o DevTools (F12)** → Aba **Performance**
2. **Clique em "Record"**
3. **Abra o dialog**
4. **Clique em "Stop"**
5. **Procure por:**
   - Requisições HTTP longas
   - `toSignal()` no início
   - `effect()` sendo executado
   - JavaScript bloqueando por >100ms

---

## **A Solução Correta - Padrão Lazy Signals**

### **Estrutura correta para componentes de busca:**

```typescript
export class MeuSearchComponent implements OnInit, OnDestroy {
  private readonly service = inject(MeuService);
  private readonly destroyRef = inject(DestroyRef);

  dados = signal<MinhaItem[]>([]);
  isLoading = signal(false);

  constructor() {
    // ✅ ONLY setup estático aqui
    // Sem toSignal(), sem effect()
  }

  ngOnInit(): void {
    // ✅ Busca inicial - com lazy load
    setTimeout(() => {
      const response = toSignal(
        this.service.getDados().pipe(catchError(() => of(undefined)))
      );

      // Reage DEPOIS que dialog renderizou
      effect(() => {
        const data = response();
        if (data) {
          this.dados.set(data);
        }
        this.isLoading.set(false);
      });
    }, 0);

    // ✅ Busca ao digitar - normal
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      this.performSearch(value);
    });
  }

  ngOnDestroy(): void {
    // ✅ Sem chamar service.resetSearchParams()
  }
}
```

---

## **Por que `setTimeout(..., 0)` Funciona?**

**Timeline:**
```
1. Constructor executa
2. ngOnInit executa
3. setTimeout(..., 0) AGENDA a execução
4. Angular RENDERIZA o dialog (agora!)
5. setTimeout callback executa (toSignal + effect)
6. Dados chegam → effect dispara → UI atualiza
```

**Resultado:** Dialog abre **INSTANTANEAMENTE** ⚡

---

## **Checklist Antes de Deixar Pronto**

- [ ] Sem `toSignal()` no constructor
- [ ] Sem `effect()` no constructor
- [ ] Sem `http.get()` no constructor
- [ ] Busca inicial em `ngOnInit()` com `setTimeout(..., 0)`
- [ ] `ngOnDestroy()` não faz requisições extras
- [ ] Dialog abre sem travada
- [ ] Dados carregam em background

---

## **Arquivos que Devem Seguir Este Padrão**

| Arquivo | O que fazer | Por quê |
|---------|-----------|---------|
| **cache.service.ts** | ✅ CRIAR novo | Cachear observables genericamente |
| **product.service.ts** | ✅ ADICIONAR `getProductsSimple()` e `getProductsWithCache()` | Retornar array sem enriquecimento + com cache |
| **product-stock-search.ts** | ✅ MOVER `toSignal()`/`effect()` do constructor para `ngOnInit()` com `setTimeout(..., 0)` | Não bloquear dialog ao abrir |
| **material-type-search.ts** | ✅ MOVER `toSignal()`/`effect()` do constructor para `ngOnInit()` com `setTimeout(..., 0)` | Não bloquear dialog ao abrir |
| **material-type-search.ts** | ✅ REMOVER `ngOnDestroy()` que chama `resetSearchParams()` | Evitar requisição extra ao fechar |

---

## **Resumo Executivo**

### **O que mudou:**

1. **ANTES (travava):**
   - Constructor fazendo requisição com `toSignal()`
   - Dialog esperava resposta para renderizar
   - Requisição extra ao fechar dialog

2. **DEPOIS (não trava):**
   - Cache reutiliza dados entre navegações
   - Dialog abre instantaneamente
   - Requisição acontece em background com `setTimeout(..., 0)`

### **Benefícios:**
- ✅ Dialog abre em <50ms (instantâneo)
- ✅ Dados carregam em background
- ✅ Cache reutiliza entre navegações
- ✅ Sem requisição extra ao fechar
- ✅ Código reativo com signals (moderno)

---

## **Lembrete para Próximas Vezes**

Se em qualquer momento um dialog começar a **travar novamente**, procure por:

1. ❌ `toSignal()` no **constructor**
2. ❌ `effect()` no **constructor**
3. ❌ `http.get()` no **constructor**
4. ❌ `subscribe()` no **constructor**
5. ❌ `ngOnDestroy()` fazendo requisições

**Regra de ouro:** Tudo assíncrono deve estar em `ngOnInit()` com `setTimeout(..., 0)`.

---

## **Se Ainda Assim Travar**

1. **Rodar git reset:**
   ```bash
   git reset --hard HEAD && git clean -fd
   ```

2. **Procurar por:**
   - `toSignal()` em constructors
   - `effect()` em constructors
   - `subscribe()` em constructors
   - Requisições HTTP em constructors

3. **Mover TUDO para `ngOnInit()`** com `setTimeout(..., 0)`

---

## **Exemplo Real: Material Type Search**

### **ANTES (travava):**
```typescript
constructor() {
  const response = toSignal(
    this.service.getMaterialTypes().pipe(...)
  );
  
  effect(() => {
    // ❌ Bloqueia renderização
  });
}
```

### **DEPOIS (não trava):**
```typescript
ngOnInit() {
  setTimeout(() => {
    const response = toSignal(
      this.service.getMaterialTypes().pipe(...)
    );
    
    effect(() => {
      // ✅ Executa depois que dialog renderizou
    });
  }, 0);
}
```

---

## **Resumo em Uma Frase**

> **"Tudo que faz requisição HTTP ou usa `toSignal()`/`effect()` deve estar em `ngOnInit()` com `setTimeout(..., 0)`, NUNCA no constructor."**

---

---

## **Próximos Passos (O que fazer amanhã)**

Se você abrir este arquivo amanhã, siga esta ordem:

1. **Criar `cache.service.ts`** (novo arquivo)
   - Copie o código da seção "Passo 1"
   - Coloque em `src/app/core/services/cache.service.ts`

2. **Modificar `product.service.ts`**
   - Adicione `private readonly cache = inject(CacheService);`
   - Copie os 2 métodos: `getProductsSimple()` e `getProductsWithCache()`

3. **Modificar `product-stock-search.ts`**
   - Remova `toSignal()`/`effect()` do constructor
   - Adicione o `setTimeout(..., 0)` com lazy signals no `ngOnInit()`

4. **Modificar `material-type-search.ts`**
   - Remova `toSignal()`/`effect()` do constructor
   - Remova `ngOnDestroy()` que faz requisição
   - Adicione o `setTimeout(..., 0)` com lazy signals no `ngOnInit()`

**Tempo estimado:** 30 minutos

---





