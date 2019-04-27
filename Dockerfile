FROM java:8
EXPOSE 8088

ADD target/app.jar /app.jar

CMD java -jar /app.jar -m sampler.rest
