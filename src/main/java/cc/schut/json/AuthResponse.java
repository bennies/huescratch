package cc.schut.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponse {

    private AuthError error;

    private AuthSuccess success;

    public AuthError getError() {
        return error;
    }

    public void setError(AuthError error) {
        this.error = error;
    }

    public AuthSuccess getSuccess() {
        return success;
    }

    public void setSuccess(AuthSuccess success) {
        this.success = success;
    }
}
