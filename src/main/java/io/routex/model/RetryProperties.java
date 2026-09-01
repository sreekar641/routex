package io.routex.model;

import lombok.Data;

@Data
public class RetryProperties {


    private int maximumRedeliveries = 3;

    private long redeliveryDelay = 1000;

    private long maximumRedeliveryDelay = 30000;

    private boolean exponentialBackOff = true;

    private double backOffMultiplier = 2.0;

    private boolean collisionAvoidance = false;

    private double collisionAvoidanceFactor = 0.15;

    private boolean logRetryAttempted = true;

    private boolean logExhausted = true;
}
