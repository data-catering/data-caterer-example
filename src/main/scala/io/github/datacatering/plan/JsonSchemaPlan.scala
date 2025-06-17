package io.github.datacatering.plan

import io.github.datacatering.datacaterer.api.PlanRun

class JsonSchemaPlanRun extends PlanRun {

  val accountTask = json("customer_accounts", "/opt/app/data/customer/json-schema", Map("saveMode" -> "overwrite"))
    .fields(metadataSource.jsonSchema("/opt/app/mount/json-schema/complex-user-schema.json"))
    .includeFields("id", "profile.name", "profile.email", "profile.age", "addresses.street", "addresses.city", "addresses.coordinates")
    .count(count.records(100))

  val conf = configuration.enableGeneratePlanAndTasks(true)
    .generatedReportsFolderPath("/opt/app/data/report")

  execute(conf, accountTask)
}
