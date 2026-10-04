FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=50", \
  "-XX:+UseSerialGC", \
  "-XX:TieredStopAtLevel=1", \
  "-Xss512k", \
  "-Xshare:auto", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]