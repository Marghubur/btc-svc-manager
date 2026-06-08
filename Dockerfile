# Stage 1: Build the application
FROM eclipse-temurin:17-jre
WORKDIR /app
EXPOSE 7801

# Copy the pre-built jar from the runner build context
COPY --from=MAVEN /build/target/btc-svc-manager.jar /app/

# Receive env from CI/CD build args
ARG BUILD_CONFIG=prod

# Use environment variable (Spring Boot automatically binds this to spring.profiles.active)
ENV SPRING_PROFILES_ACTIVE=$BUILD_CONFIG

ENTRYPOINT ["java", "-jar", "btc-svc-manager.jar"]