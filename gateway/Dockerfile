# Building Container

FROM gradle:4.10-jdk8 as GradleBuilder

USER 0

COPY ca-certificates/* /usr/local/share/ca-certificates/
RUN update-ca-certificates

RUN apt-get update \
    && apt-get install -y \
        maven \
    && rm -rf /var/lib/apt/lists/*

USER gradle

ENV APP_NAME=gateway

COPY --chown=gradle:gradle build.gradle /home/gradle/src/build.gradle
COPY --chown=gradle:gradle gradle.properties /home/gradle/src/gradle.properties
COPY --chown=gradle:gradle settings.gradle /home/gradle/src/settings.gradle
COPY --chown=gradle:gradle ${APP_NAME} /home/gradle/src/${APP_NAME}

WORKDIR /home/gradle/src

ENV GRADLE_USER_HOME=/home/gradle

RUN gradle :${APP_NAME}:installDist \
--no-daemon --info --stacktrace \
-PincludePravegaCredentials=true

# Runtime Container

FROM openjdk:8-jre

ENV APP_NAME=gateway
ENV GATEWAY_URI=http://0.0.0.0:8080/

ENV pravega_client_auth_method=Bearer
ENV pravega_client_auth_loadDynamic=true

EXPOSE 8080

COPY --from=GradleBuilder /home/gradle/src/${APP_NAME}/build/install/${APP_NAME} /opt/${APP_NAME}
ENTRYPOINT /opt/${APP_NAME}/bin/${APP_NAME}
