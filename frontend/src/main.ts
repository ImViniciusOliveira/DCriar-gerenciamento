import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

/**
 * Ponto de entrada (entrypoint) da aplicação Angular.
 *
 * A função `bootstrapApplication` inicializa a aplicação, usando:
 * - `App`: O componente raiz (root) que será renderizado.
 * - `appConfig`: O objeto de configuração que fornece todas as dependências
 *   e funcionalidades globais, como rotas, cliente HTTP e interceptadores.
 */
bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
