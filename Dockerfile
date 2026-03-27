# Build stage
FROM maven:3.8.5-openjdk-8 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM payara/micro:5.2022.5

USER root
RUN apk add --no-cache fontconfig ttf-dejavu
USER payara

# Payara Micro standard deployment directory
# By naming it ROOT.war, it automatically deploys to "/"
COPY --from=build /app/target/ticket-system.war ${DEPLOY_DIR}/ROOT.war

# Set memory limits and disable clustering using environment variables
# This is more compatible with Payara's default entrypoint
ENV JVM_ARGS="-Xmx256m -Xms256m -XX:+UseSerialGC -Djava.awt.headless=true"
ENV PAYARA_MICRO_OPTIONS="--nocluster --deploymentDir ${DEPLOY_DIR}"
ENV HZ_NETWORK_RESTAPI_ENABLED=true

EXPOSE 8080
