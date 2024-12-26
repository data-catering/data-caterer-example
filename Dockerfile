FROM datacatering/data-caterer:0.13.1

COPY --chown=app:app build/libs/data-caterer-example-0.1.0.jar /opt/app/job.jar
