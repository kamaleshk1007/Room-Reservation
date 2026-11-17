# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/room-reservation-0.0.1-SNAPSHOT.war /app/room-reservation.war

EXPOSE 8080
ENV PORT=8080

# The artifact is a Spring Boot repackaged WAR, so it runs as an executable jar:
# java -jar. WAR packaging is retained for the college Tomcat deployment.
ENTRYPOINT ["java", "-jar", "/app/room-reservation.war"]
