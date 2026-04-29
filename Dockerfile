FROM maven:3.6.0-jdk-11-slim AS build

ARG NEXUS_USER
ARG NEXUS_PASS

RUN mkdir -p /root/.m2
ADD settings.xml /root/.m2/

WORKDIR /app
COPY  . .
RUN mvn -T 4C clean -DskipTests install

#second stage
FROM adoptopenjdk/openjdk11:x86_64-alpine-jdk-11.0.16.1_1-slim

## jasper font ttf-dejavu config
RUN apk --update add fontconfig ttf-dejavu
RUN apk --no-cache add msttcorefonts-installer fontconfig && \
    update-ms-fonts && \
    fc-cache -f

EXPOSE 8080
COPY --from=build /app/target/vaccinale-ms-*.jar /app/vaccinale-ms.jar


# Run the jar file
ENTRYPOINT ["java","-Duser.timezone=Europe/Rome", "-jar", "/app/vaccinale-ms.jar"]