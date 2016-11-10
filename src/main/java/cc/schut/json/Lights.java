package cc.schut.json;

import com.fasterxml.jackson.annotation.*;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Lights {
    @JsonProperty
    private Map<String, Light> lights = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Light> allLights() {
        return lights;
    }

    @JsonAnySetter
    public void set(String name, Light value) {
        lights.put(name, value);
    }

}
