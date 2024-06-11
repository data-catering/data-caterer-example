FROM datacatering/data-caterer-basic:0.10.8

COPY --chown=app:app build/libs/data-caterer-example-0.1.0.jar /opt/spark/jars/data-caterer.jar
