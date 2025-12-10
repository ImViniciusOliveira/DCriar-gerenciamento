export const environment = {
  production: false,
  /**
   * Deriva a URL da API a partir do hostname atual.
   * Isso evita o hardcoding de 'localhost' e permite que o frontend
   * se conecte ao backend a partir de qualquer dispositivo na mesma rede.
   *
   * Se o hostname não puder ser determinado (ex: em ambientes sem 'window'),
   * um erro será lançado, forçando a identificação e correção do problema.
   */
  apiUrl: ((): string => {
    // Tenta obter o hostname do ambiente atual.
    const host = globalThis?.location?.hostname;

    if (!host) {
      // Se o hostname não puder ser determinado, lança um erro explícito.
      // Isso garante que problemas de configuração de ambiente sejam identificados imediatamente.
      throw new Error('Não foi possível determinar o hostname da API. Verifique a configuração do ambiente.');
    }

    // Constrói a URL da API usando o hostname determinado.
    return `http://${host}:8080`;
  })(),
};
