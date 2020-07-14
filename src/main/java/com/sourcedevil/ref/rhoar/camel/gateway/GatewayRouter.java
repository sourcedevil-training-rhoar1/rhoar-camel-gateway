package com.sourcedevil.ref.rhoar.camel.gateway;

import java.util.ArrayList;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.util.toolbox.AggregationStrategies;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A simple Camel route that triggers from a timer and calls a bean and prints to system out.
 * <p/>
 * Use <tt>@Component</tt> to make Camel auto detect this route when starting.
 */
@Component
@ConfigurationProperties(prefix="gateway")
public class GatewayRouter extends RouteBuilder {

   
    
    private static final String REST_ENDPOINT_ECHO= "http4:echo-api.3scale.net?httpClient.connectTimeout=1000" +
            "&bridgeEndpoint=true" +
            "&copyHeaders=true" +
            "&connectionClose=true";
    
    private static final String REST_ENDPOINT_FINTO= "http4:api.finto.fi/rest/v1/vocabularies?httpClient.connectTimeout=1000" +
            "&bridgeEndpoint=true" +
            "&copyHeaders=true" +
            "&connectionClose=true";

    private static final String REST_ENDPOINT_WINNER= "http4:sourcedevil-th1-raffle-api-sourcedevil-th1-raffle-api.apps.us-west-1.starter.openshift-online.com/raffle/raffle/ganador?httpClient.connectTimeout=1000" +
            "&bridgeEndpoint=true" +
            "&copyHeaders=true" +
            "&connectionClose=true";

    @Override
    public void configure() {
        from("direct:echoServiceUrl").streamCaching()
                .to(REST_ENDPOINT_ECHO)
                .log("Response from Microprofile microservice: " +
                        "${body}")
                .convertBodyTo(String.class)
                .end();

        from("direct:fintoServiceUrl").streamCaching()
                .to(REST_ENDPOINT_FINTO)
                .log("Response from Spring Boot microservice: " +
                        "${body}")
                .convertBodyTo(String.class)
                .end();

        from("direct:winnerServiceUrl").streamCaching()
                .to(REST_ENDPOINT_WINNER)
                .log("Response from Spring Boot microservice: " +
                        "${body}")
                .convertBodyTo(String.class)
                .end();

        rest()
            .get("/gateway").enableCORS(true)            
            .route()
                .multicast(AggregationStrategies.flexible()
                        .accumulateInCollection(ArrayList.class))
                .parallelProcessing()
                    .to("direct:fintoServiceUrl")
                    .to("direct:echoServiceUrl")
                .end()
            .marshal().json(JsonLibrary.Jackson)
            .convertBodyTo(String.class)
        .endRest();
                
        rest()
        .get("/finto").enableCORS(true)            
        .to("direct:fintoServiceUrl");
        
        rest()
        .get("/winner").enableCORS(true)            
        .to("direct:winnerServiceUrl");

        rest()
        .get("/echo").enableCORS(true)
        .to("direct:echoServiceUrl");
        
        rest("/health").get("").produces("application/json").to("direct:health");

        from("direct:health").routeId("HealthREST")
                .log("Health endpoint invoked")
                .setBody().simple("{\n"
                    + "  status: Ready\n"
                    + "  server: " + System.getenv("HOSTNAME") + "\n"
                    + "}\n");
    }

   
}
