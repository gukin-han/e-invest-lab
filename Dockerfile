# Stage 1: Build frontend
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build backend
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon -q > /dev/null 2>&1 || true
COPY src/ src/
COPY --from=frontend /app/frontend/dist/ src/main/resources/static/
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar -x test --no-daemon -Dorg.gradle.jvmargs="-Xmx1g"

# Stage 3: Runtime — pdftotext(poppler) 는 EPS 파서가 프로세스로 호출한다
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends poppler-utils \
    && rm -rf /var/lib/apt/lists/*
RUN sed -i 's/jdk.tls.disabledAlgorithms=.*/jdk.tls.disabledAlgorithms=/' $JAVA_HOME/conf/security/java.security
COPY --from=backend /app/build/libs/e-invest-lab-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
