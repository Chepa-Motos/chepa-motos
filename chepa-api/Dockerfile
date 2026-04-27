FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN ./gradlew dependencies --no-daemon -q 2>/dev/null || true
COPY src src
RUN ./gradlew bootJar -x test --no-daemon -q

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
