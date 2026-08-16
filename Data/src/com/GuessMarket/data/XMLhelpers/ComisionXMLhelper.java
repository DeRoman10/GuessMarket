package com.GuessMarket.data.XMLhelpers;

import jakarta.xml.bind.annotation.*;
import com.GuessMarket.data.enums.CommissionType;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class ComisionXMLhelper implements Serializable {

    @XmlAttribute(name = "type")
    private CommissionType type;

    @XmlValue
    private int value;

    public ComisionXMLhelper() {}

    public CommissionType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }
}