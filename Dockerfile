FROM adoptopenjdk:11-jre-hotspot
RUN mkdir /opt/app
COPY target/ciphernode.jar /opt/app
CMD ["java", "-jar", "/opt/app/cipherable-server.jar"]
