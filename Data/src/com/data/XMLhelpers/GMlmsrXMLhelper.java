package com.data.XMLhelpers;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class GMlmsrXMLhelper {
    @XmlElement(name = "b")
    private int b;

    public GMlmsrXMLhelper() {}

    public GMlmsrXMLhelper(int b) {
        this.b = b;
    }

    public int getB() {
        return b;
    }
}
