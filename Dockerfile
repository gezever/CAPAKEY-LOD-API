FROM openjdk:17-jdk-alpine
COPY target/capakeylodapi-0.0.1-SNAPSHOT.jar capakeylodapi-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/capakeylodapi-0.0.1-SNAPSHOT.jar"]
