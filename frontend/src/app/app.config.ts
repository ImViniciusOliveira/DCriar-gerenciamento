import { ApplicationConfig, provideZonelessChangeDetection, LOCALE_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth-interceptor';

// Registra o locale 'pt' para que o Angular possa formatar números e datas em português do Brasil.
registerLocaleData(pt);

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

    // Define o LOCALE_ID para 'pt-BR' para toda a aplicação.
    { provide: LOCALE_ID, useValue: 'pt-BR' }
  ],
};
