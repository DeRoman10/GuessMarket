package servlet.events;

import com.maneger.SystemManager;
import com.data.events.entities.Event;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = "/events")
public class EventServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        SystemManager systemManager = (SystemManager) getServletContext().getAttribute("systemManager");

        if (systemManager == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson("Error: SystemManager not initialized."));
            return;
        }

        String eventName = request.getParameter("name");

        try {
            if (eventName != null && !eventName.trim().isEmpty()) {
                String detailedStatus = systemManager.getEventDetailedStatus(eventName.trim());
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(gson.toJson(detailedStatus));
            } else {
                List<Event> activeEvents = systemManager.getEvents();
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(gson.toJson(activeEvents));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(e.getMessage()));
        }
    }
}