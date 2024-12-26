package io.github.datacatering.plan

import io.github.datacatering.datacaterer.api.PlanRun
import io.github.datacatering.datacaterer.api.model.{ArrayType, DateType, DoubleType, IntegerType}

import java.sql.Date

class AdvancedPlanRun extends PlanRun {

  val startDate = Date.valueOf("2022-01-01")
  val accountIdField = field.name("account_id").regex("ACC[0-9]{8}")
  val nameField = field.name("name").expression("#{Name.name}")

  val postgresTask = task.name("customer_postgres")
    .steps(
      step
        .name("transaction")
        .jdbcTable("account.transaction")
        .fields(
          accountIdField,
          field.name("txn_id").regex("txn_[0-9]{5}"),
          field.name("year").`type`(IntegerType).sql("YEAR(date)"),
          nameField,
          field.name("date").`type`(DateType).min(startDate),
          field.name("amount").`type`(DoubleType).max(10000),
          field.name("credit_debit").sql("CASE WHEN amount < 0 THEN 'C' ELSE 'D' END"),
        )
        .count(
          count
            .recordsPerFieldGenerator(generator.min(1).max(10), "account_id")
        )
      ,
      step
        .name("account")
        .jdbcTable("account.account")
        .fields(
          accountIdField,
          nameField,
          field.name("open_date").`type`(DateType).min(startDate),
          field.name("status").oneOf("open", "closed", "pending")
        )
        .count(10)
    )

  val jsonTask = task.name("account_json")
    .steps(
      step
        .name("account_info")
        .path("src/main/resources/sample/json")
        .fields(
          accountIdField,
          nameField,
          field.name("txn_list")
            .`type`(ArrayType)
            .fields(
              field.name("id").sql("_holding_txn_id"),
              field.name("date").`type`(DateType).min(startDate),
              field.name("amount").`type`(DoubleType),
            ),
          field.name("_holding_txn_id").omit(true)
        )
    )

  val conf = configuration.postgres("my_postgres")

  val accountPlan = plan.name("Create accounts and transactions across Postgres and JSON file")
    .taskSummaries(
      taskSummary.dataSource("my_postgres").task(postgresTask),
      taskSummary.dataSource("my_json").task(jsonTask)
    )
    .addForeignKeyRelationship(
      foreignField("my_postgres", "transaction", "txn_id"),
      foreignField("my_json", "account_info", "_holding_txn_id")
    )
    .addForeignKeyRelationship(
      foreignField("my_postgres", "account", "account_id"),
      foreignField("my_postgres", "transaction", "account_id"),
      foreignField("my_json", "account_info", "account_id"),
    )
    .addForeignKeyRelationship(
      foreignField("my_postgres", "account", "name"),
      foreignField("my_postgres", "transaction", "name"),
      foreignField("my_json", "account_info", "name"),
    )

  execute(accountPlan, conf)
}
