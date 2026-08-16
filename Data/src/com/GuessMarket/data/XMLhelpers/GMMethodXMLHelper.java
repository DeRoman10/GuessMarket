package com.GuessMarket.data.XMLhelpers;
import jakarta.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class GMMethodXMLHelper {
    @XmlElement(name = "GM-LMSR")
    private GMlmsrXMLhelper lmsr;

    public GMMethodXMLHelper() {}

    public GMlmsrXMLhelper getLmsr() {
        return lmsr;
    }
}
