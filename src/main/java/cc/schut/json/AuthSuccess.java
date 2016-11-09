package cc.schut.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthSuccess {

    @JsonProperty
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
