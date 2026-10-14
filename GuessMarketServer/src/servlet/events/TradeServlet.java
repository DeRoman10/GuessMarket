
package servlet.events;

import com.data.events.entities.Event;
import com.data.events.enums.OrderDirection;
import com.data.users.entities.User;
import com.maneger.PurchaseReceipt;
import com.maneger.SystemManager;
import com.maneger.TradeMonitor;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.util.ApiUtils;

import java.io.IOException;

@WebServlet(urlPatterns = "/trade")
public class TradeServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User me = ApiUtils.requireUser(request, response);
        if (me == null) return;

        SystemManager sm = ApiUtils.manager(request);
        try {
            String eventName = request.getParameter("eventName");
            int optionIndex = Integer.parseInt(request.getParameter("optionIndex"));
            int amount = Integer.parseInt(request.getParameter("amount"));
            if (amount <= 0) throw new Exception("Amount must be a positive whole number.");

            Event event = sm.getEvent(eventName);
            TradeMonitor<PurchaseReceipt> monitor;

            if (event.isLmsr()) {
                monitor = sm.executePurchase(eventName, optionIndex, amount, me);
            } else {
                double limitPrice = Double.parseDouble(request.getParameter("limitPrice"));
                if (!Double.isFinite(limitPrice) || limitPrice <= 0) throw new Exception("Price must be a positive number.");
                OrderDirection direction;
                try {
                    direction = OrderDirection.valueOf(request.getParameter("direction").trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new Exception("Direction must be BUY or SELL.");
                }
                monitor = sm.executeOBTrade(eventName, optionIndex, amount, limitPrice, direction, me);
            }

            PurchaseReceipt receipt = monitor.waitForResult();
            ApiUtils.write(response, HttpServletResponse.SC_OK, receipt);

        } catch (NumberFormatException | NullPointerException e) {
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing trade parameters.");
        } catch (Exception e) {
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
}

