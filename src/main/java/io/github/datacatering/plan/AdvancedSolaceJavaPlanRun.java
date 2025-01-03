package io.github.datacatering.plan;

import io.github.datacatering.datacaterer.api.model.ArrayType;
import io.github.datacatering.datacaterer.api.model.DateType;
import io.github.datacatering.datacaterer.api.model.DoubleType;
import io.github.datacatering.datacaterer.api.model.HeaderType;
import io.github.datacatering.datacaterer.api.model.IntegerType;
import io.github.datacatering.datacaterer.api.model.TimestampType;
import io.github.datacatering.datacaterer.javaapi.api.PlanRun;

import java.sql.Date;

public class AdvancedSolaceJavaPlanRun extends PlanRun {
    {
        var solaceTask = solace("my_solace", "smf://host.docker.internal:55554")
                .destination("/JNDI/Q/rest_test_queue")
                .fields(
                        field().name("value").sql("TO_JSON(content)"),
                        //field().name("partition").type(IntegerType.instance()),   //can define message JMS priority here
                        field().name("headers") //set message properties via headers field
                                .type(HeaderType.getType())
                                .sql(
                                        "ARRAY(" +
                                                "NAMED_STRUCT('key', 'account-id', 'value', TO_BINARY(content.account_id, 'utf-8'))," +
                                                "NAMED_STRUCT('key', 'updated', 'value', TO_BINARY(content.details.updated_by.time, 'utf-8'))" +
                                                ")"
                                ),
                        field().name("content")
                                .fields(
                                        field().name("account_id").regex("ACC[0-9]{8}"),
                                        field().name("year").type(IntegerType.instance()).min(2021).max(2023),
                                        field().name("amount").type(DoubleType.instance()),
                                        field().name("details")
                                                .fields(
                                                        field().name("name").expression("#{Name.name}"),
                                                        field().name("first_txn_date").type(DateType.instance()).sql("ELEMENT_AT(SORT_ARRAY(content.transactions.txn_date), 1)"),
                                                        field().name("updated_by")
                                                                .fields(
                                                                        field().name("user"),
                                                                        field().name("time").type(TimestampType.instance())
                                                                )
                                                ),
                                        field().name("transactions").type(ArrayType.instance())
                                                .fields(
                                                        field().name("txn_date").type(DateType.instance()).min(Date.valueOf("2021-01-01")).max("2021-12-31"),
                                                        field().name("amount").type(DoubleType.instance())
                                                )
                                )
                )
                .count(count().records(10));

        var conf = configuration()
                .generatedReportsFolderPath("/opt/app/data/report");

        execute(conf, solaceTask);
    }
}
