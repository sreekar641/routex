package io.routex.model;

import lombok.Data;

@Data
public class Endpoint {

    private String name;

    private EndpointType type;

    private String topic;

    private String groupId;
}
