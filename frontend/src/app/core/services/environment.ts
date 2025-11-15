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
      console.error('Failed to derive API host from location.hostname, falling back to localhost:', e);
      return 'http://localhost:8080';
    }
  })(),
};
