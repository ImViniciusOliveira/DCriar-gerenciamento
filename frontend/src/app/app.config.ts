import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth-interceptor';

/**
 * Configuração global da aplicação Angular, definindo os provedores (providers)
 * que estarão disponíveis para todos os componentes.
 * Esta é a abordagem moderna para configurar uma aplicação standalone.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    // Ativa o modo "zoneless" do Angular, uma estratégia de detecção de mudanças
    // mais moderna e performática que depende de Sinais e não do Zone.js.
    provideZonelessChangeDetection(),

    // Configura o roteador da aplicação com as rotas definidas em `app.routes.ts`.
    provideRouter(routes),

    // Configura o HttpClient global e registra os interceptadores.
    // `withInterceptors` é a forma moderna e funcional de adicionar interceptadores.
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
