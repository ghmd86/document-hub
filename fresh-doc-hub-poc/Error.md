Was getting the following error with com.fasterxml.jackson.databind.JsonNode;

`2025-12-04 16:01:50 - c.d.service.DocumentEnquiryService - Building error response: Could not read property @org.springframework.data.relational.core.mapping.Column("data_extraction_config")private com.fasterxml.jackson.databind.JsonNode com.documenthub.entity.MasterTemplateDefinitionEntity.dataExtractionConfig from column data_extraction_config!
2025-12-04 16:01:50 - c.d.service.DocumentEnquiryService - Response built: 0 documents, page 1/0, processing time: 0ms`

Our project was using: r2dbc-postgresql-0.9.2.RELEASE.jar

When I upgraded to: r2dbc-postgresql-1.0.7.RELEASE.jar

used the import io.r2dbc.postgresql.codec.Json; class instead of JsonNode

I was getting the following error:

`
An attempt was made to call a method that does not exist. The attempt was made from the following location:

    io.r2dbc.postgresql.SingleHostConnectionFunction.getCredentials(SingleHostConnectionFunction.java:72)

The following method did not exist:

    'reactor.core.publisher.Mono reactor.core.publisher.Mono.singleOptional()'

The calling method's class, io.r2dbc.postgresql.SingleHostConnectionFunction, was loaded from the following location:

    jar:file:/C:/Users/tmohammed/.m2/repository/org/postgresql/r2dbc-postgresql/1.0.7.RELEASE/r2dbc-postgresql-1.0.7.RELEASE.jar!/io/r2dbc/postgresql/SingleHostConnectionFunction.class

The called method's class, reactor.core.publisher.Mono, is available from the following locations:

    jar:file:/C:/Users/tmohammed/.m2/repository/io/projectreactor/reactor-core/3.4.34/reactor-core-3.4.34.jar!/reactor/core/publisher/Mono.class

The called method's class hierarchy was loaded from the following locations:

    reactor.core.publisher.Mono: file:/C:/Users/tmohammed/.m2/repository/io/projectreactor/reactor-core/3.4.34/reactor-core-3.4.34.jar`


Not sure how to solve it?

The other projects use codec.Json.