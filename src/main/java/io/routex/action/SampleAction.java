package io.routex.action;

import org.apache.camel.Exchange;

@ActionHandler("sampleAction")
public class SampleAction implements Action {
    @Override
    public void handle(Exchange exchange) throws Exception {
        Object body = exchange.getIn().getBody();
        String out = "SampleAction processed: " + (body == null ? "<null>" : body.toString());
        exchange.getMessage().setBody(out);
    }
}
