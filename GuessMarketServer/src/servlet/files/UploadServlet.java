package servlet.files;

import com.data.files.entities.Market;
import com.data.users.entities.User;
import com.handlers.XmlFileControl;
import com.maneger.SystemManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import jakarta.xml.bind.JAXBContext;
import servlet.util.ApiUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = "/upload")
@MultipartConfig(fileSizeThreshold = 5 * 1024 * 1024, maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class UploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User uploader = ApiUtils.requireUser(request, response);
        if (uploader == null) return;

        SystemManager sm = ApiUtils.manager(request);
        try {
            Part filePart = request.getPart("file");
            if (filePart == null) {
                ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, "Error: No file uploaded.");
                return;
            }
            XmlFileControl.validateFileName(filePart.getSubmittedFileName());

            Market market;
            try {
                byte[] bytes;
                try (InputStream in = filePart.getInputStream()) {
                    bytes = in.readAllBytes();
                }
                String xml = new String(bytes, StandardCharsets.UTF_8)
                        .replaceAll("xsi:[a-zA-Z]+=\"[^\"]*\"", "");

                market = (Market) JAXBContext.newInstance(Market.class)
                        .createUnmarshaller().unmarshal(new StringReader(xml));
            } catch (Exception parseError) {
                parseError.printStackTrace();
                throw new Exception("The file is not a valid Guess Market XML file (" + rootMessage(parseError) + ").");
            }


            XmlFileControl.validateMarketData(market);

            synchronized (sm) {
                sm.loadMarketData(market, uploader);
            }
            ApiUtils.write(response, HttpServletResponse.SC_OK,
                    "File is valid. " + market.getEvents().size() + " event(s) were added; you are their Market Maker.");

        } catch (Exception e) {
            e.printStackTrace();
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, "Upload Failed: " + e.getMessage());
        }
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }
}