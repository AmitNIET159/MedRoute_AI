# =====================================================
# MedRoute AI — Multi-Stage Docker Build
# Stage 1: Maven build → WAR
# Stage 2: Tomcat 9 runtime (javax.servlet compatible)
# =====================================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Cache Maven dependencies first (faster rebuilds)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy source and build WAR (skip tests for deploy)
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ---- Stage 2: Runtime ----
FROM tomcat:9.0-jdk17-temurin-jammy

# Remove Tomcat default webapps (security best practice)
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy built WAR as ROOT app (serves at /)
COPY --from=builder /build/target/MedRouteAI.war /usr/local/tomcat/webapps/ROOT.war

# Copy startup script
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Render sets PORT env var; default to 8080 for local Docker
ENV PORT=8080

EXPOSE ${PORT}

CMD ["/docker-entrypoint.sh"]
