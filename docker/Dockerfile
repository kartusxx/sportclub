# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace

COPY . .
# -pl собирает один модуль, -am — родительский pom и зависимые модули; кэш ~/.m2 живёт между сборками
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests -pl ${MODULE} -am package

# ---- runtime stage ----
FROM eclipse-temurin:21-jre-alpine
ARG MODULE
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar
# Лимит памяти JVM берётся из ограничений контейнера; при нехватке памяти процесс завершается
# и контейнер перезапускается, а не «зависает»
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]