FROM eclipse-temurin:21-jdk-alpine

ARG MONGODB_USERNAME
ENV MONGODB_USERNAME $MONGODB_USERNAME

ARG MONGODB_PASSWORD
ENV MONGODB_PASSWORD $MONGODB_PASSWORD

ARG MONGODB_HOST
ENV MONGODB_HOST $MONGODB_HOST

EXPOSE 8080 9090
ADD fastfood-order/target/fastfood-*.jar /opt/api.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar", "/opt/api.jar"]

