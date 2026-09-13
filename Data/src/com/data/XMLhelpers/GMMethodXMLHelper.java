package com.data.XMLhelpers;
import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class GMMethodXMLHelper {
    @XmlElement(name = "GM-LMSR")
    private GMlmsrXMLhelper lmsr;

    @XmlElement(name = "GM-order-book")
    private GMOrderBookXMLHelper orderBook;

    public GMMethodXMLHelper() {}

    public GMMethodXMLHelper(GMlmsrXMLhelper lmsr) {
        this.lmsr = lmsr;
    }

    public GMMethodXMLHelper(GMOrderBookXMLHelper orderBook) {
        this.orderBook = orderBook;
    }

    public GMlmsrXMLhelper getLmsr() {
        return lmsr;
    }
    public GMOrderBookXMLHelper getOrderBook() { return orderBook; }
}
