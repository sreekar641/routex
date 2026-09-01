package io.routex.service;

import io.routex.config.RouteXProperties;
import io.routex.model.Endpoint;
import io.routex.model.Route;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RouteConfigurer {

    private final CamelContext camelContext;

    private final ApplicationContext applicationContext;

    private final RouteXProperties properties;

    public RouteConfigurer(
            CamelContext camelContext,
            ApplicationContext applicationContext,
            RouteXProperties properties) {

        this.camelContext = camelContext;
        this.applicationContext = applicationContext;
        this.properties = properties;

    }

    public void configureRoutes() throws Exception {

        Map<String, Endpoint> endpoints =
                properties.getEndpoints()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Endpoint::getName,
                                        Function.identity()));

        for (Route route : properties.getRoutes()) {
            if (!route.isEnabled()) {
                log.info("Skipping disabled route: {}", route.getEffectiveId());
                continue;
            }

            // support two styles:
            // 1) endpoint-referenced: route.source / route.target reference endpoints defined in properties
            // 2) direct-URI (legacy): route.from / route.to are raw URIs

            if (route.getSource() != null) {
                Endpoint source = getEndpoint(endpoints, route.getSource());
                Endpoint target = null;
                if (route.getTarget() != null) {
                    target = getEndpoint(endpoints, route.getTarget());
                }
                log.info("Configuring route '{}' as {}: source='{}' ({}) -> target='{}' ({})",
                        route.getEffectiveId(),
                        classifyRouteType(source, target),
                        source.getName(),
                        source.getType(),
                        target != null ? target.getName() : "N/A",
                        target != null ? target.getType() : "N/A");
                registerRoute(route, source, target);
            } else if (route.getEffectiveFrom() != null) {
                log.info("Configuring direct route '{}' as {}: from='{}' -> to='{}'",
                        route.getEffectiveId(),
                        classifyDirectRoute(route.getEffectiveFrom(), route.getEffectiveTo()),
                        route.getEffectiveFrom(),
                        route.getEffectiveTo() != null ? route.getEffectiveTo() : "N/A");
                registerDirectRoute(route);
            } else {
                log.warn("Skipping route with no source and no 'from' URI: {}", route.getEffectiveId());
            }
        }
    }

    private Endpoint getEndpoint(
            Map<String, Endpoint> endpoints,
            String name) {

        Endpoint endpoint = endpoints.get(name);

        if (endpoint == null) {

            throw new IllegalArgumentException(
                    "Endpoint not found: " + name);
        }

        return endpoint;
    }

    private void registerRoute(
            Route route,
            Endpoint source,
            Endpoint target) throws Exception {

        org.apache.camel.builder.RouteBuilder rb = new org.apache.camel.builder.RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String fromUri = buildUri(source);
                String rid = route.getEffectiveId() != null ? route.getEffectiveId() : (route.getName() != null ? route.getName() : route.getSource());
                org.apache.camel.model.RouteDefinition rd = from(fromUri).routeId(rid != null ? rid : java.util.UUID.randomUUID().toString());

                if (route.getActionBean() != null && !route.getActionBean().isEmpty()) {
                    log.info("Route '{}' ({}) invokes action bean '{}' before forwarding",
                            rid,
                            classifyRouteType(source, target),
                            route.getActionBean());
                    rd.process(exchange -> {
                        io.routex.action.Action action = (io.routex.action.Action) applicationContext.getBean(route.getActionBean());
                        action.handle(exchange);
                    });
                }

                if (target != null) {
                    String toUri = buildUri(target);
                    log.info("Route '{}' ({}) forwarding from {} to {}",
                            rid,
                            classifyRouteType(source, target),
                            fromUri,
                            toUri);
                    rd.to(toUri);
                } else {
                    log.info("Route '{}' ({}) created from {} with no explicit target endpoint",
                            rid,
                            classifyRouteType(source, target),
                            fromUri);
                }
            }
        };

        camelContext.addRoutes(rb);
        log.info("Route '{}' registered in Camel context: category={}, sourceType={}, targetType={}",
                route.getEffectiveId() != null ? route.getEffectiveId() : route.getName(),
                classifyRouteType(source, target),
                source != null ? source.getType() : "DIRECT",
                target != null ? target.getType() : "N/A");
    }

    private String buildUri(Endpoint endpoint) {
        switch (endpoint.getType()) {
            case KAFKA:
                // simple Kafka URI using topic and groupId
                return "kafka:" + endpoint.getTopic() + "?brokers=${kafka.bootstrap-servers}" + (endpoint.getGroupId() != null ? "&groupId=" + endpoint.getGroupId() : "");
            case HTTP:
                // HTTP endpoint — treat topic as path
                return "jetty:" + (endpoint.getTopic() != null ? endpoint.getTopic() : "/");
            case JMS:
                return "jms:queue:" + endpoint.getTopic();
            default:
                // fallback to raw topic as direct endpoint
                return "direct:" + (endpoint.getTopic() != null ? endpoint.getTopic() : endpoint.getName());
        }
    }

    // public API for admin use
    public synchronized void addRoute(Route route) throws Exception {
        if (route.getSource() != null) {
            Map<String, Endpoint> endpoints =
                    properties.getEndpoints()
                            .stream()
                            .collect(
                                    Collectors.toMap(
                                            Endpoint::getName,
                                            Function.identity()));

            Endpoint source = getEndpoint(endpoints, route.getSource());
            Endpoint target = null;
            if (route.getTarget() != null) {
                target = getEndpoint(endpoints, route.getTarget());
            }

            registerRoute(route, source, target);
        } else if (route.getEffectiveFrom() != null) {
            registerDirectRoute(route);
        } else {
            throw new IllegalArgumentException("Route must specify either source endpoint name or a direct 'from' URI");
        }
    }

    public synchronized void removeRoute(String routeId) throws Exception {
        camelContext.removeRoute(routeId);
    }

    private void registerDirectRoute(Route route) throws Exception {
        org.apache.camel.builder.RouteBuilder rb = new org.apache.camel.builder.RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String rid = route.getEffectiveId() != null ? route.getEffectiveId() : route.getName();
                org.apache.camel.model.RouteDefinition rd = from(route.getEffectiveFrom()).routeId(rid != null ? rid : java.util.UUID.randomUUID().toString());

                if (route.getActionBean() != null && !route.getActionBean().isEmpty()) {
                    log.info("Direct route '{}' ({}) invokes action bean '{}' before forwarding",
                            rid,
                            classifyDirectRoute(route.getEffectiveFrom(), route.getEffectiveTo()),
                            route.getActionBean());
                    rd.process(exchange -> {
                        io.routex.action.Action action = (io.routex.action.Action) applicationContext.getBean(route.getActionBean());
                        action.handle(exchange);
                    });
                }

                String toUri = route.getEffectiveTo();
                if (toUri != null) {
                    log.info("Direct route '{}' ({}) forwarding from {} to {}",
                            rid,
                            classifyDirectRoute(route.getEffectiveFrom(), route.getEffectiveTo()),
                            route.getEffectiveFrom(),
                            toUri);
                    rd.to(toUri);
                } else {
                    log.info("Direct route '{}' ({}) created from {} with no explicit target URI",
                            rid,
                            classifyDirectRoute(route.getEffectiveFrom(), route.getEffectiveTo()),
                            route.getEffectiveFrom());
                }
            }
        };

        camelContext.addRoutes(rb);
        log.info("Direct route '{}' registered in Camel context: category={}, from={}, to={}",
                route.getEffectiveId() != null ? route.getEffectiveId() : route.getName(),
                classifyDirectRoute(route.getEffectiveFrom(), route.getEffectiveTo()),
                route.getEffectiveFrom(),
                route.getEffectiveTo() != null ? route.getEffectiveTo() : "N/A");
    }

    private String classifyRouteType(Endpoint source, Endpoint target) {
        boolean isHttp = (source != null && source.getType() == io.routex.model.EndpointType.HTTP)
                || (target != null && target.getType() == io.routex.model.EndpointType.HTTP);
        boolean isMessaging = (source != null && (source.getType() == io.routex.model.EndpointType.KAFKA || source.getType() == io.routex.model.EndpointType.JMS))
                || (target != null && (target.getType() == io.routex.model.EndpointType.KAFKA || target.getType() == io.routex.model.EndpointType.JMS));
        if (isHttp) return "REST";
        if (isMessaging) return "MESSAGING";
        return "INTEGRATION";
    }

    private String classifyDirectRoute(String fromUri, String toUri) {
        if (fromUri != null && fromUri.toLowerCase().startsWith("kafka:")) return "MESSAGING";
        if (fromUri != null && fromUri.toLowerCase().startsWith("jms:")) return "MESSAGING";
        if (fromUri != null && fromUri.toLowerCase().startsWith("http") || (toUri != null && toUri.toLowerCase().startsWith("http"))) return "REST";
        if (fromUri != null && fromUri.toLowerCase().startsWith("jetty:")) return "REST";
        return "INTEGRATION";
    }

}
