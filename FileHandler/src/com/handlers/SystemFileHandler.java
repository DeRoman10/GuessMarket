package com.handlers;

import com.data.files.entities.Market;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;
import java.nio.charset.StandardCharsets;

public abstract class SystemFileHandler {

    public static Market loadXmlData(InputStream inputStream) throws Exception {
        JAXBContext context = JAXBContext.newInstance(Market.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        String xmlContent = new String(buffer.toByteArray(), StandardCharsets.UTF_8);

        xmlContent = xmlContent.replace("\uFEFF", "");

        xmlContent = xmlContent.replaceFirst("<Guess-Market", "<Guess-Market xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");

        try (StringReader sr = new StringReader(xmlContent)) {
            return (Market) unmarshaller.unmarshal(sr);
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