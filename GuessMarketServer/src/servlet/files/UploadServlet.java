package servlet.files;

import com.data.files.entities.Market;
import com.data.users.entities.User;
import com.handlers.*;
import com.maneger.SystemManager;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;

@WebServlet(urlPatterns = "/upload")
@MultipartConfig
public class UploadServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        SystemManager systemManager = (SystemManager) getServletContext().getAttribute("systemManager");

        String loggedInUsername = (String) request.getSession().getAttribute("loggedInUser");
        if (loggedInUsername == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson("Error: User not logged in."));
            return;
        }

        try {
            Part filePart = request.getPart("file");
            if (filePart == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson("Error: No file uploaded."));
                return;
            }

            Market loadedMarket;
            try (InputStream fileContent = filePart.getInputStream()) {
                loadedMarket = SystemFileHandler.loadXmlData(fileContent);
            }

            XmlFileControl.validateMarketData(loadedMarket);
            User uploader = systemManager.getUsers().stream()
                    .filter(u -> u.getName().equalsIgnoreCase(loggedInUsername))
                    .findFirst()
                    .orElseThrow(() -> new Exception("Uploader user not found in system."));

            synchronized (systemManager) {
                systemManager.loadMarketData(loadedMarket, uploader);
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson("File uploaded and processed successfully."));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson("Upload Failed: " + e.getMessage()));
        }
    }
}