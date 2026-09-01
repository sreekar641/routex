package io.routex.camel;

import io.routex.config.RouteXProperties;
import io.routex.model.Route;
import io.routex.service.RouteConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
public class DynamicRouteLoader {

    @Autowired
    private RouteConfigurer routeConfigurer;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() throws Exception {
        // delegate to RouteConfigurer to register routes from RouteXProperties
        routeConfigurer.configureRoutes();
    }

    public synchronized void addRoute(Route route) throws Exception {
        routeConfigurer.addRoute(route);
    }

    public synchronized void removeRoute(String routeId) throws Exception {
        routeConfigurer.removeRoute(routeId);
    }
}
