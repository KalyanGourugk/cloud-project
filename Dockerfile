FROM openjdk:8
EXPOSE 8080
ADD target/navigators.jar navigators.jar
ENTRYPOINT ["java","-jar","/navigators.jar"]