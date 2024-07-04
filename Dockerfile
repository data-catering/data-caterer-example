FROM datacatering/data-caterer-basic:0.11.6

COPY --chown=app:app build/libs/data-caterer-example-0.1.0.jar /opt/app/job.jar
