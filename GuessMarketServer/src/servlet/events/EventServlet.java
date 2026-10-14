package servlet.events;

import com.data.events.entities.Event;
import com.maneger.SystemManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.util.ApiUtils;

import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = "/events")
public class EventServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (ApiUtils.requireUser(request, response) == null) return;

        SystemManager sm = ApiUtils.manager(request);
        String eventName = request.getParameter("name");

        try {
            synchronized (sm) {
                if (eventName != null && !eventName.trim().isEmpty()) {
                    ApiUtils.write(response, HttpServletResponse.SC_OK, sm.getEventDetailedStatus(eventName.trim()));
                } else {
                    List<Event> events = sm.getEvents();
                    ApiUtils.write(response, HttpServletResponse.SC_OK, events);
                }
            }
        } catch (Exception e) {
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
}
