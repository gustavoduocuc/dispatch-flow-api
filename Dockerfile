# Requiere carpeta wallet/ en el contexto de build (CI: ORACLE_WALLET_BASE64; local: ./run-docker).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src ./src
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/dispatch-flow-api-0.0.1-SNAPSHOT.jar /app/app.jar
COPY wallet/ /app/wallet/
ENV TNS_ADMIN=/app/wallet
EXPOSE 8080
VOLUME /app/efs
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
