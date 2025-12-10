import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Interceptor HTTP para adicionar cabeçalhos de autenticação às requisições.
 *
 * Esta função intercepta todas as requisições HTTP de saída e pode modificá-las,
 * adicionando um token JWT ao cabeçalho `Authorization`.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req);
};
