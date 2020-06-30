FROM gradle:4.10-jdk8

USER 0

COPY ca-certificates/* /usr/local/share/ca-certificates/
RUN update-ca-certificates

RUN apt-get update \
    && apt-get install -y \
        maven \
    && rm -rf /var/lib/apt/lists/*

USER gradle

ENV APP_NAME=flinkprocessor

COPY --chown=gradle:gradle build.gradle /home/gradle/src/build.gradle
COPY --chown=gradle:gradle gradle.properties /home/gradle/src/gradle.properties
COPY --chown=gradle:gradle settings.gradle /home/gradle/src/settings.gradle
COPY --chown=gradle:gradle ${APP_NAME} /home/gradle/src/${APP_NAME}

WORKDIR /home/gradle/src

ENV GRADLE_USER_HOME=/home/gradle

# Build and publish the shadow jar to the Maven repo
RUN gradle :${APP_NAME}:publish \
--no-daemon --info --stacktrace \
-PincludePravegaCredentials=true
