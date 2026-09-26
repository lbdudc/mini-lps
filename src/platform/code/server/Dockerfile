FROM bellsoft/liberica-runtime-container:jdk-17-musl

ARG JAR_NAME

ENV JAR_NAME ${JAR_NAME}

WORKDIR /app/server

RUN apk add --no-cache ca-certificates

COPY . .

# The Gradle distribution and every Maven dependency live in GRADLE_USER_HOME, which is a
# BuildKit cache mount: they are downloaded once per machine instead of on every build.
# bootJar (not `clean build`) builds only the runnable jar -- no plain jar, distributions or
# check tasks -- and spotlessApply (compileJava depends on it) is skipped: it only reformats
# sources, and would download google-java-format for nothing.
# `sh gradlew`, not `./gradlew`: a deployment uploaded from another machine (a zip made on
# Windows) loses the script's executable bit, and the build failed with exit code 126.
ENV GRADLE_USER_HOME=/gradle-cache
RUN --mount=type=cache,target=/gradle-cache \
    sh ./gradlew bootJar -x test -x spotlessApply --no-daemon

CMD java -jar build/libs/${JAR_NAME}.jar --spring.profiles.active=prod
