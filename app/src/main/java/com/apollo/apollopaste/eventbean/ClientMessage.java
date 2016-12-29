package com.apollo.apollopaste.eventbean;

/**
 * Created by zayh_yf20160909 on 2016/12/29.
 */

public class ClientMessage {
    private String message;

    public ClientMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
