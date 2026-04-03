interface AppEnvironment {
  production: boolean;
  apiBasePath: string;
  apiVersionPath: string;
}

export const environment: AppEnvironment = {
  production: false,
  // O frontend sempre fala com a API por caminho relativo.
  // Em dev, o Angular proxy encaminha /api para o backend local.
  // Em produção, o nginx do frontend encaminha /api para o backend na rede interna.
  apiBasePath: '/api',
  apiVersionPath: '/api/v1',
};
