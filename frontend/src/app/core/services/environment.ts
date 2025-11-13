export const environment = {
  production: false,
  // Em builds locais/produção, use o hostname atual para derivar a URL da API.
  // Isso evita hardcode de `localhost` e permite acessar de outros dispositivos na mesma rede.
  apiUrl: ((): string => {
    try {
      const host = window && window.location && window.location.hostname ? window.location.hostname : 'localhost';
      // porta do backend em produção é 8080 (mapeada pelo compose)
      return `http://${host}:8080`;
    } catch (e) {
      return 'http://localhost:8080';
    }
  })(),
};
