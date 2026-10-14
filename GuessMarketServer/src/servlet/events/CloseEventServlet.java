package servlet.events;

import com.data.events.entities.Event;
import com.data.users.entities.User;
import com.maneger.FinalReceipt;
import com.maneger.SystemManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.util.ApiUtils;

import java.io.IOException;

@WebServlet(urlPatterns = "/closeEvent")
public class CloseEventServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User me = ApiUtils.requireUser(request, response);
        if (me == null) return;

        SystemManager sm = ApiUtils.manager(request);
        try {
            String eventName = request.getParameter("eventName");
            int winningIndex = Integer.parseInt(request.getParameter("winningOptionIndex"));

            Event event = sm.getEvent(eventName);
            FinalReceipt receipt = sm.closeEvent(eventName, winningIndex, me).waitForResult();   // MM check inside
            ApiUtils.write(response, HttpServletResponse.SC_OK, receipt.getReceiptAsString(event.getCommissionRate()));

        } catch (NumberFormatException | NullPointerException e) {
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing event / winning option.");
        } catch (Exception e) {
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
}
