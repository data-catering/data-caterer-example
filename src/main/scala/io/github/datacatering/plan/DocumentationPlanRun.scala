package io.github.datacatering.plan

import io.github.datacatering.datacaterer.api.PlanRun
import io.github.datacatering.datacaterer.api.model.{ArrayType, DateType, DoubleType, IntegerType, TimestampType}

import java.sql.{Date, Timestamp}
import java.time.LocalDate

class DocumentationPlanRun extends PlanRun {

  val baseFolder = "/opt/app/data"
  val accountStatus = List("open", "closed", "pending", "suspended")
  val jsonTask = json("account_info", s"$baseFolder/json", Map("saveMode" -> "overwrite"))
    .schema(
      field.name("account_id").regex("ACC[0-9]{8}"),
      field.name("year").`type`(IntegerType).sql("YEAR(date)"),
      field.name("balance").`type`(DoubleType).min(10).max(1000),
      field.name("date").`type`(DateType).min(Date.valueOf(LocalDate.now())),
      field.name("status").sql("element_at(sort_array(update_history, false), 1).status"),
      field.name("update_history")
        .`type`(ArrayType)
        .arrayMinLength(1)
        .schema(
          field.name("updated_time").`type`(TimestampType).min(Timestamp.valueOf("2022-01-01 00:00:00")),
          field.name("status").oneOf(accountStatus: _*),
        ),
      field.name("customer_details")
        .schema(
          field.name("name").sql("_join_txn_name"),
          field.name("age").`type`(IntegerType).min(18).max(90),
          field.name("city").expression("#{Address.city}")
        ),
      field.name("_join_txn_name").expression("#{Name.name}").omit(true)
    )
    .count(count.records(100))
    .validations(
      validation.expr("year >= 2022")
    )

  val csvTxns = csv("transactions", s"$baseFolder/csv", Map("saveMode" -> "overwrite", "header" -> "true"))
    .schema(
      field.name("account_id"),
      field.name("txn_id"),
      field.name("name"),
      field.name("amount").`type`(DoubleType).min(10).max(100),
      field.name("merchant").expression("#{Company.name}"),
    )
    .count(
      count
        .records(100)
        .recordsPerColumnGenerator(generator.min(1).max(2), "account_id", "name")
    )
    .validations(
      validation.expr("LENGTH(merchant) > 0").description("Merchant should not be empty"),
      validation.expr("amount > 15").description("Most amounts should be greater than $15").errorThreshold(5)
    )

  val postgresValidateTask = postgres("my_postgres", "jdbc:postgresql://host.docker.internal:5432/customer")
    .table("account", "accounts")
    .validations(
      validation.col("account_id").isNotNull,
      validation.col("name").matches("[A-Z][a-z]+ [A-Z][a-z]+").errorThreshold(0.2).description("Some names have different formats"),
      validation.col("balance").greaterThanOrEqual(0).errorThreshold(10).description("Account can have negative balance if overdraft"),
      validation.expr("CASE WHEN status == 'closed' THEN isNotNull(close_date) ELSE isNull(close_date) END"),
      validation.unique("account_id", "name"),
      validation.groupBy("account_id", "name").max("login_retry").lessThan(10)
    )

  val foreignKeySetup = plan
    .addForeignKeyRelationship(jsonTask, List("account_id", "_join_txn_name"), List((csvTxns, List("account_id", "name"))))

  val conf = configuration
    .generatedReportsFolderPath(baseFolder + "/report")
    .enableSinkMetadata(true)
    .enableValidation(true)

  execute(foreignKeySetup, conf, jsonTask, csvTxns)
}
