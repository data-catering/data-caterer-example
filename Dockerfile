FROM datacatering/data-caterer:0.14.5

COPY --chown=app:app build/libs/data-caterer-example-0.1.0.jar /opt/app/job.jar
