package com.example.finalprojectvegan.Fcm;

import com.example.finalprojectvegan.Fcm.NotificationData;
import com.google.gson.annotations.SerializedName;

public class PushNotification {
    @SerializedName("notification")
    public NotificationData notificationData;

    @SerializedName("to")
    public String to;

    public PushNotification(NotificationData notificationData, String to) {
        this.notificationData = notificationData;
        this.to = to;
    }

    public NotificationData getNotificationData() {
        return notificationData;
    }

    public void setNotificationData(NotificationData notificationData) {
        this.notificationData = notificationData;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}