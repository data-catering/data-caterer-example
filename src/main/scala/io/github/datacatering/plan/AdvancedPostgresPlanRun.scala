package io.github.datacatering.plan

import io.github.datacatering.datacaterer.api.PlanRun

class AdvancedPostgresPlanRun extends PlanRun {

  val accountTask = postgres("customer_postgres", "jdbc:postgresql://host.docker.internal:5432/customer")
    .schema(field.name("account_number").regex("[0-9]{10}"))

  val config = configuration
    .generatedReportsFolderPath("/opt/app/data/report")
    .enableGeneratePlanAndTasks(true)
    .enableRecordTracking(true)
    .enableDeleteGeneratedRecords(false)
    .enableGenerateData(true)

  execute(config, accountTask)
}
