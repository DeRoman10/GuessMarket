package com.GuessMarket.data.enums;
import jakarta.xml.bind.annotation.XmlEnumValue;

public enum CommissionType {

    @XmlEnumValue("on-purchase")
    ON_PURCHASE,

    @XmlEnumValue("on-close")
    ON_CLOSE
}
