package com.GuessMarket.data.XMLhelpers;

import jakarta.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class GMlmsrXMLhelper {
    @XmlElement(name = "b")
    private int b;

    public GMlmsrXMLhelper() {}

    public int getB() {
        return b;
    }
}
