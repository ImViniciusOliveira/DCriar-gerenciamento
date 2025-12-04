export const environment = {
  production: false,
  /**
   * Deriva a URL da API a partir do hostname atual.
   * Isso evita o hardcoding de 'localhost' e permite que o frontend
   * se conecte ao backend a partir de qualquer dispositivo na mesma rede.
   */
  apiUrl: ((): string => {
    try {
      const host = globalThis?.location?.hostname ? globalThis.location.hostname : 'localhost';
      return `http://${host}:8080`;
    } catch (e: any) {
      // Em ambientes sem 'window' (como renderização no servidor), usa um fallback seguro.
      console.error('Falha ao derivar o host da API a partir de location.hostname, usando localhost como fallback:', e);
      return 'http://localhost:8080';
    }
  })(),
};
