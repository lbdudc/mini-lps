FROM bellsoft/liberica-runtime-container:jdk-17-musl

ARG JAR_NAME

ENV JAR_NAME ${JAR_NAME}

WORKDIR /app/server

RUN apk add --no-cache ca-certificates

COPY . .

RUN ./gradlew clean build -x test --no-daemon

CMD java -jar build/libs/${JAR_NAME}.jar --spring.profiles.active=prod
