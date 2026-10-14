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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = "/marketData")
public class MarketDataServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (ApiUtils.requireUser(request, response) == null) return;

        SystemManager sm = ApiUtils.manager(request);
        String action = request.getParameter("action");
        String eventName = request.getParameter("eventName");

        try {
            synchronized (sm) {
                Map<String, Object> data = new HashMap<>();

                if ("orderbook".equals(action)) {
                    data.put("bids", sm.getEventBids(eventName));
                    data.put("asks", sm.getEventAsks(eventName));

                } else if ("summary".equals(action)) {
                    Event event = sm.getEvent(eventName);
                    List<Double> prices = new ArrayList<>();
                    List<Map<String, Double>> obStats = new ArrayList<>();

                    for (int i = 0; i < event.getOptions().size(); i++) {
                        prices.add(sm.getOptionCurrentPrice(eventName, i));
                        if (event.isOrderBook()) obStats.add(sm.getOptionOBStats(eventName, i));
                    }
                    data.put("prices", prices);
                    if (event.isOrderBook()) {
                        data.put("obStats", obStats);
                        data.put("participants", sm.getOBParticipantsOverview(eventName));
                    }
                } else if ("costPreview".equals(action)) {
                    int optionIndex = Integer.parseInt(request.getParameter("optionIndex"));
                    int amount = Integer.parseInt(request.getParameter("amount"));
                    double cost = sm.getPurchaseCostPreview(eventName, optionIndex, amount);
                    data.put("cost", cost);
                } else {
                    ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, "Unknown action.");
                    return;
                }
                ApiUtils.write(response, HttpServletResponse.SC_OK, data);
            }
        } catch (Exception e) {
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
}
