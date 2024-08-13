FROM amazoncorretto:22-alpine

WORKDIR /usr/src/app

COPY ./target/app.jar /usr/src/app/

ENV JAVA_TOOL_OPTIONS="-XX:FlightRecorderOptions=stackdepth=256 \
 -XX:+UseContainerSupport \
 -XX:MaxRAMPercentage=90.0"

EXPOSE 8080

CMD ["java", "-server", "-jar", "/usr/src/app/app.jar"]