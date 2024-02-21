package com.desay.devicescan;

public class QrCodeItem {
    public boolean isEditable = false;
    public String key = "";
    public String value = "";

    @Override
    public String toString() {
        return "QrCodeItem{" +
                "isEditable=" + isEditable +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
