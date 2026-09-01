# routex

A small Spring Boot + Apache Camel starter that dynamically creates routes by scanning annotated Action beans and reading YAML route definitions. Focus: Kafka initially; pluggable for other endpoints.

Usage:
- Build: mvn package
- Run: java -jar target/routex-0.1.0-SNAPSHOT.jar
- Admin API: POST /admin/routes to add a route (JSON matching DynamicRoutesProperties.RouteConfig)

Design:
- Annotate beans with @ActionHandler("beanName") and implement io.routex.action.Action
- Define startup routes in application.yml under `routex.routes`
- Runtime admin API to add/remove routes

This is a scaffold for further features: route validation, auth for admin API, UI, connectors, and persistence for dynamic config.
