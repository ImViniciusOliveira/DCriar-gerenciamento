# backend/Dockerfile
# Imagem runtime simples: copia o JAR pré-compilado em backend/target para executar

FROM eclipse-temurin:21-jre
WORKDIR /app

# Copia o JAR já construído (gerado localmente por mvn package)
COPY target/dcriar-api-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
