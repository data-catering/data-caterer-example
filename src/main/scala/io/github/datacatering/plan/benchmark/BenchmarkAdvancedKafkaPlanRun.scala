package io.github.datacatering.plan.benchmark

import io.github.datacatering.datacaterer.api.PlanRun
import io.github.datacatering.datacaterer.api.model.{ArrayType, DateType, DoubleType, IntegerType, TimestampType}

import java.sql.Date

class BenchmarkAdvancedKafkaPlanRun extends PlanRun {

  val recordCount = System.getenv("RECORD_COUNT").toInt

  val kafkaTask = kafka("my_kafka", "kafkaserver:29092")
    .topic("account-topic")
    .fields(
      field.name("key").sql("content.account_id"),
      field.name("value").sql("TO_JSON(content)"),
      field.name("headers")
        .`type`(ArrayType)
        .sql(
          """ARRAY(
            |  NAMED_STRUCT('key', 'account-id', 'value', TO_BINARY(content.account_id, 'utf-8')),
            |  NAMED_STRUCT('key', 'updated', 'value', TO_BINARY(content.details.updated_by.time, 'utf-8'))
            |)""".stripMargin
        ),
      field.name("content")
        .fields(
          field.name("account_id").regex("ACC[0-9]{8}"),
          field.name("year").`type`(IntegerType).min(2021).max(2023),
          field.name("amount").`type`(DoubleType),
          field.name("details")
            .fields(
              field.name("name").expression("#{Name.name}"),
              field.name("first_txn_date").`type`(DateType).sql("ELEMENT_AT(SORT_ARRAY(content.transactions.txn_date), 1)"),
              field.name("updated_by")
                .fields(
                  field.name("user"),
                  field.name("time").`type`(TimestampType),
                ),
            ),
          field.name("transactions").`type`(ArrayType)
            .fields(
              field.name("txn_date").`type`(DateType).min(Date.valueOf("2021-01-01")).max("2021-12-31"),
              field.name("amount").`type`(DoubleType),
            )
        ),
      field.name("tmp_year").sql("content.year").omit(true),
      field.name("tmp_name").sql("content.details.name").omit(true)
    )
    .count(count.records(recordCount))

  val conf = configuration
    .enableSaveReports(false)
    .enableCount(false)

  val basePlan = plan.seed(1)

  execute(basePlan, conf, kafkaTask)
}
