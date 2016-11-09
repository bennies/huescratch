package cc.schut.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthError {

    @JsonProperty
    private int type;
    @JsonProperty
    private String address;
    @JsonProperty
    private String description;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
