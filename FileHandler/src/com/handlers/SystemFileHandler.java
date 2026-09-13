package com.handlers;

import com.data.files.entities.Market;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public abstract class SystemFileHandler {

    public static Market loadXmlData(String filePath) throws Exception {
        JAXBContext context = JAXBContext.newInstance(Market.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath))) {
            XMLStreamReader xsr = xif.createXMLStreamReader(bis);
            return (Market) unmarshaller.unmarshal(xsr);
        }
    }

    public static void saveXmlData(Market market, String filePath) throws Exception {
        JAXBContext context = JAXBContext.newInstance(Market.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            marshaller.marshal(market, bos);
        }
    }
}