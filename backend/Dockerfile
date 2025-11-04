# backend/Dockerfile

# --- ESTÁGIO 1: O "BUILDER" ---
# Usamos a imagem completa do Maven com JDK 21 para construir o projeto
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 1) Copia o pom.xml primeiro (cacheia as dependências)
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# 2) Copia o resto do código-fonte
COPY src ./src

# 3) Roda o build (skip tests para acelerar; testes devem rodar no CI)
RUN mvn clean package -DskipTests -B

# --- ESTÁGIO 2: A IMAGEM FINAL (LIMPA) ---
# Imagem de runtime menor que contém apenas o JRE
FROM eclipse-temurin:21-jre
WORKDIR /app

# 4) Copia apenas o jar gerado do estágio builder
COPY --from=builder /app/target/dcriar-api-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
