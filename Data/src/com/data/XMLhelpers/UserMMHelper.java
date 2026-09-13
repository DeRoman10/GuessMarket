package com.data.XMLhelpers;

import jakarta.xml.bind.annotation.*;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class UserMMHelper implements Serializable{
    @XmlAttribute(name = "id")
    private String id;

    public UserMMHelper() {}
    public String getId() { return id; }
}
