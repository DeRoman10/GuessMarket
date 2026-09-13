package com.data.XMLhelpers;

import jakarta.xml.bind.annotation.*;
import com.data.events.enums.CommissionType;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class CommissionXMLHelper implements Serializable {

    @XmlAttribute(name = "type")
    private CommissionType type;

    @XmlValue
    private int value;

    public CommissionXMLHelper() {}

    public CommissionXMLHelper(CommissionType type, int value) {
        this.type = type;
        this.value = value;
    }

    public CommissionType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }
}
