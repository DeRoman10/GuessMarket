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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = "/marketData")
public class MarketDataServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        SystemManager systemManager = (SystemManager) getServletContext().getAttribute("systemManager");
        String loggedInUsername = (String) request.getSession().getAttribute("loggedInUser");

        if (loggedInUsername == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String action = request.getParameter("action");
        String eventName = request.getParameter("eventName");

        try {
            synchronized (systemManager) {
                if ("orderbook".equals(action)) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("bids", systemManager.getEventBids(eventName));
                    data.put("asks", systemManager.getEventAsks(eventName));
                    response.getWriter().write(gson.toJson(data));

                } else if ("summary".equals(action)) {
                    Event event = systemManager.getEvent(eventName);
                    Map<String, Object> data = new HashMap<>();

                    List<Double> prices = new ArrayList<>();
                    List<Map<String, Double>> obStats = new ArrayList<>();

                    for (int i = 0; i < event.getOptions().size(); i++) {
                        prices.add(systemManager.getOptionCurrentPrice(eventName, i));
                        if (event.isOrderBook()) {
                            obStats.add(systemManager.getOptionOBStats(eventName, i));
                        }
                    }

                    data.put("prices", prices);
                    if (event.isOrderBook()) {
                        data.put("obStats", obStats);
                        data.put("participants", systemManager.getOBParticipantsOverview(eventName));
                    }
                    response.getWriter().write(gson.toJson(data));
                }
            }
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(e.getMessage()));
        }
    }
}