FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY libs libs
COPY services services
RUN --mount=type=cache,target=/root/.m2 bash ./mvnw -B -pl services/eureka-server -am package -DskipTests
FROM eclipse-temurin:21-jre
# curl se utiliza en el healthcheck de compose
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/services/eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
