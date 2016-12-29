package com.apollo.apollopaste.eventbean;

/**
 * Created by zayh_yf20160909 on 2016/12/29.
 */

public class ClientOfflineNotice {
    String notice;

    public ClientOfflineNotice(String notice) {
        this.notice = notice;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }
}
