package io.github.datacatering.plan

import io.github.datacatering.datacaterer.api.PlanRun
import io.github.datacatering.datacaterer.api.model.Constants.{ROWS_PER_SECOND, VALIDATION_IDENTIFIER}

class AdvancedHttpPlanRun extends PlanRun {

  val httpTask = http("my_http", options = Map(ROWS_PER_SECOND -> "1"))
    .schema(metadataSource.openApi("/opt/app/mount/http/petstore.json"))
    .schema(field.name("bodyContent").schema(field.name("id").regex("ID[0-9]{8}")))
    .count(count.records(2))

  val httpGetTask = http("get_http", options = Map(VALIDATION_IDENTIFIER -> "GET/pets/{id}"))
    .validations(
      validation.col("request.method").isEqual("GET"),
      validation.col("request.method").isEqualCol("response.statusText"),
      validation.col("response.statusCode").isEqual(200),
      validation.col("response.headers.Content-Length").greaterThan(0),
      validation.col("response.headers.Content-Type").isEqual("application/json"),
    )

  val myPlan = plan.addForeignKeyRelationship(
    foreignField("my_http", "POST/pets", "bodyContent.id"),
    foreignField("my_http", "DELETE/pets/{id}", "pathParamid"),
    foreignField("my_http", "GET/pets/{id}", "pathParamid")
  )

  val conf = configuration.enableGeneratePlanAndTasks(true)
    .generatedReportsFolderPath("/opt/app/data/report")

  execute(myPlan, conf, httpTask, httpGetTask)
}
