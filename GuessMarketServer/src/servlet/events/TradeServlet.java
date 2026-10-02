package servlet.events;

import com.maneger.SystemManager;

import com.data.events.enums.OrderDirection;
import com.data.users.entities.User;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = "/trade")
public class TradeServlet extends HttpServlet {

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

        String eventName = request.getParameter("eventName");
        String optionIndexStr = request.getParameter("optionIndex");
        String amountStr = request.getParameter("amount");
        String limitPriceStr = request.getParameter("limitPrice");
        String directionStr = request.getParameter("direction");

        try {
            int optionIndex = Integer.parseInt(optionIndexStr);
            int amount = Integer.parseInt(amountStr);

            User user = systemManager.getUsers().stream()
                    .filter(u -> u.getName().equalsIgnoreCase(loggedInUsername))
                    .findFirst()
                    .orElseThrow(() -> new Exception("User not found in system."));

            boolean isLmsr = systemManager.getEvent(eventName).isLmsr();

            Object receipt;

            if (isLmsr) {
                receipt = systemManager.executePurchase(eventName, optionIndex, amount, user).waitForResult();
            } else {
                double limitPrice = Double.parseDouble(limitPriceStr);
                OrderDirection direction = OrderDirection.valueOf(directionStr.toUpperCase());
                receipt = systemManager.executeOBTrade(eventName, optionIndex, amount, limitPrice, direction, user).waitForResult();
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(receipt));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(e.getMessage()));
        }
    }
}
