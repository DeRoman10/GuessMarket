package com.data.XMLhelpers;

import jakarta.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class GMOrderBookXMLHelper implements Serializable {
    @XmlAttribute(name = "allow-mint")
    private boolean allowMint;

    @XmlAttribute(name = "initial")
    private Integer initial; // Changed to Integer to check for null

    @XmlAttribute(name = "inital")
    private Integer inital;

    @XmlAttribute(name = "d")
    private int d;

    public GMOrderBookXMLHelper() {}

    public GMOrderBookXMLHelper(boolean allowMint, int initial, int d) {
        this.allowMint = allowMint;
        this.initial = initial;
        this.d = d;
    }

    public boolean isAllowMint() { return allowMint; }

    public int getInitial() {
        if (initial != null) return initial;
        if (inital != null) return inital;
        return 0;
    }

    public int getD() { return d; }
}
