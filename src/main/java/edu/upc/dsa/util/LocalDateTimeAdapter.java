package edu.upc.dsa.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;

public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {
    @Override
    public LocalDateTime unmarshal(String value) {
        return value == null ? null : LocalDateTime.parse(value);
    }

    @Override
    public String marshal(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
