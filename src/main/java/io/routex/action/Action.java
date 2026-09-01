package io.routex.action;

import org.apache.camel.Exchange;

public interface Action {
    void handle(Exchange exchange) throws Exception;
}
