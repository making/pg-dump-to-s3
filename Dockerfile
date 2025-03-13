FROM eclipse-temurin:17-jdk as builder
WORKDIR application
ADD ./.mvn .mvn/
ADD ./mvnw mvnw
ADD ./pom.xml pom.xml
ADD ./src src/
RUN ./mvnw -V clean package -DskipTests --no-transfer-progress && \
    cp target/*.jar application.jar && \
    java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:17-jre
WORKDIR application
RUN apt-get update -qq && \
  apt-get install -y -qq gnupg lsb-release curl ca-certificates && \
  install -d /usr/share/postgresql-common/pgdg && \
  curl -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc --fail https://www.postgresql.org/media/keys/ACCC4CF8.asc && \
  sh -c 'echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list' && \
  apt-get update -qq && \
  apt-get install -y -qq postgresql-client-16 && \
  apt-get remove --purge -y -qq gnupg && \
  rm -rf /var/lib/apt/lists/*
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
