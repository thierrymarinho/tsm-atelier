# syntax=docker/dockerfile:1

# ---- build ----------------------------------------------------------------
# O wrapper baixa o Gradle 9.6.1 declarado em gradle-wrapper.properties, então a
# imagem só precisa do JDK. O toolchain do build.gradle.kts pede Java 25.
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Dependências primeiro, numa camada própria: enquanto build.gradle.kts não
# mudar, o Docker reusa esta camada e o build sobe sem baixar o mundo de novo.
COPY gradlew ./
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---- runtime --------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine

# Usuário sem privilégio: se a aplicação for comprometida, o processo não é root
# dentro do contêiner.
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app

COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar
USER app

# `MaxRAMPercentage` faz a JVM enxergar o limite do contêiner em vez de chutar a
# partir da memória da máquina hospedeira — sem isso ela reserva demais e o
# provedor mata o processo por OOM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

# O Render injeta `PORT` (padrão 10000). O fallback mantém o mesmo comando
# funcionando localmente, onde a variável não existe.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]
