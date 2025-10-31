# ESTÁGIO 1: Build da Aplicação com Maven
# Usamos uma imagem Maven com a versão do Java correta (21) para compilar o projeto.
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Define o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copia primeiro o pom.xml para aproveitar o cache de dependências do Docker
COPY pom.xml .

# Baixa todas as dependências do projeto
RUN mvn dependency:go-offline

# Copia o resto do código-fonte da aplicação
COPY src ./src

# Compila a aplicação, cria o .jar e pula os testes (eles devem rodar no pipeline de CI)
RUN mvn clean package -DskipTests


# ESTÁGIO 2: Criação da Imagem Final
# Usamos uma imagem JRE (Java Runtime Environment) mínima para rodar a aplicação.
# Isso resulta em uma imagem muito menor e mais segura.
FROM eclipse-temurin:21-jre-alpine

# Define o diretório de trabalho
WORKDIR /app

# Copia o .jar gerado no estágio de build para a imagem final
COPY --from=build /app/target/dcriar-api-*.jar ./app.jar

# Expõe a porta em que a aplicação Spring Boot roda
EXPOSE 8080

# Comando para iniciar a aplicação quando o contêiner for executado
ENTRYPOINT ["java", "-jar", "app.jar"]
