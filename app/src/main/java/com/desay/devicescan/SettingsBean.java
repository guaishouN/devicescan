package com.desay.devicescan;

import java.util.LinkedList;
import java.util.List;

public class SettingsBean {
    public String url = "";
    public List<String> item = new LinkedList<>();
    public List<String> editable = new LinkedList<>();

    @Override
    public String toString() {
        return "SettingsBean{" +
                "url='" + url + '\'' +
                ", item=" + item +
                ", editable=" + editable +
                '}';
    }
}
