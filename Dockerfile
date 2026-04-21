FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

LABEL org.opencontainers.image.source="https://github.com/oscaruiz/myCQRS" \
      org.opencontainers.image.description="myCQRS demo application" \
      org.opencontainers.image.licenses="GPL-3.0"

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/src/demo/target/mycqrs-demo.jar app.jar
RUN chown app:app app.jar

USER app
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseG1GC", "-jar", "app.jar"]
