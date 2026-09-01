package io.routex.model;

import lombok.Data;

@Data
public class Route {

    // legacy YAML may provide 'id' or 'name'
    private String id;

    private String name;

    // endpoint reference name
    private String source;

    private String target;

    // direct URIs (legacy style)
    private String from;
    private String to;

    private boolean enabled = true;

    // optional action bean name to process messages
    private String actionBean;

    public String getEffectiveId() {
        if (id != null && !id.isEmpty()) return id;
        if (name != null && !name.isEmpty()) return name;
        return null;
    }

    public String getEffectiveFrom() {
        if (from != null && !from.isEmpty()) return from;
        return null;
    }

    public String getEffectiveTo() {
        if (to != null && !to.isEmpty()) return to;
        return null;
    }
}
