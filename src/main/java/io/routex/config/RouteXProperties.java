package io.routex.config;

import io.routex.model.Endpoint;
import io.routex.model.RetryProperties;
import io.routex.model.Route;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "routex")
public class RouteXProperties {
    private List<Endpoint> endpoints = new ArrayList<>();

    private List<Route> routes = new ArrayList<>();

    private RetryProperties retry = new RetryProperties();
}