package com.sourcedevil.ref.rhoar.camel.gateway;

import java.util.ArrayList;

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

    private String productsServiceUrl, offerServiceUrl;

    private static final String REST_ENDPOINT_OFFER=
            "http4:%s/offers/list?httpClient.connectTimeout=1000" +
                    "&bridgeEndpoint=true" +
                    "&copyHeaders=true" +
                    "&connectionClose=true";
    
    private static final String REST_ENDPOINT_PRODUCT=
            "http4:%s/camel/feature/config/64?httpClient.connectTimeout=1000" +
                    "&bridgeEndpoint=true" +
                    "&copyHeaders=true" +
                    "&connectionClose=true";

    @Override
    public void configure() {
        from("direct:offerServiceUrl").streamCaching()
                .toF(REST_ENDPOINT_OFFER, offerServiceUrl)
                .log("Response from Microprofile microservice: " +
                        "${body}")
                .convertBodyTo(String.class)
                .end();

        from("direct:productsServiceUrl").streamCaching()
                .toF(REST_ENDPOINT_PRODUCT, productsServiceUrl)
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
                    .to("direct:offerServiceUrl")
                    .to("direct:productsServiceUrl")
                .end()
            .marshal().json(JsonLibrary.Jackson)
            .convertBodyTo(String.class)
        .endRest();
        
        rest("/health").get("").produces("application/json").to("direct:health");

        from("direct:health").routeId("HealthREST")
                .log("Health endpoint invoked")
                .setBody().simple("{\n"
                    + "  status: Ready\n"
                    + "  server: " + System.getenv("HOSTNAME") + "\n"
                    + "}\n");
    }

    public void setOffersServiceUrl(String offerServiceUrl) {
        this.offerServiceUrl = offerServiceUrl;
    }

    public void setProductsServiceUrl(String productsServiceUrl) {
        this.productsServiceUrl = productsServiceUrl;
    }

}
