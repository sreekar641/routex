package io.routex.service;

import io.routex.config.RouteXProperties;
import io.routex.model.RetryProperties;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.OnExceptionDefinition;
import org.springframework.context.ApplicationContext;

public abstract class AbstractRouteBuilder extends RouteBuilder {

    protected final ApplicationContext applicationContext;
    protected final RouteXProperties routeXProperties;

    protected AbstractRouteBuilder(
            ApplicationContext applicationContext,
            CamelContext camelContext,
            RouteXProperties routeXProperties) {

        super(camelContext);

        this.applicationContext = applicationContext;
        this.routeXProperties = routeXProperties;
    }

    @Override
    public final void configure() throws Exception {

        configureExceptionHandling();

        configureRoute();
    }

    private void configureExceptionHandling() {

        RetryProperties retry = routeXProperties.getRetry();

        OnExceptionDefinition definition =
                onException(Exception.class)
                        .useOriginalMessage()
                        .maximumRedeliveries(retry.getMaximumRedeliveries())
                        .redeliveryDelay(retry.getRedeliveryDelay())
                        .maximumRedeliveryDelay(
                                retry.getMaximumRedeliveryDelay())
                        .logRetryAttempted(
                                retry.isLogRetryAttempted())
                        .logExhausted(
                                retry.isLogExhausted());

        if (retry.isExponentialBackOff()) {
            definition
                    .useExponentialBackOff()
                    .backOffMultiplier(
                            retry.getBackOffMultiplier());
        }

        if (retry.isCollisionAvoidance()) {
            definition
                    .useCollisionAvoidance()
                    .collisionAvoidanceFactor(
                            retry.getCollisionAvoidanceFactor());
        }
    }

    protected abstract void configureRoute() throws Exception;
}
