package servlet.events;

import com.data.events.enums.CommissionType;
import com.data.users.entities.User;
import com.maneger.SystemManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.util.ApiUtils;

import java.io.IOException;

@WebServlet(urlPatterns = "/createEvent")
public class CreateEventServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User me = ApiUtils.requireUser(request, response);
        if (me == null) return;

        SystemManager sm = ApiUtils.manager(request);
        try {
            String name = request.getParameter("name");
            String description = request.getParameter("description");
            int commissionRate = Integer.parseInt(request.getParameter("commissionRate"));
            CommissionType commissionType = CommissionType.valueOf(request.getParameter("commissionType"));
            String opt1 = request.getParameter("option1");
            String opt2 = request.getParameter("option2");
            String method = request.getParameter("method");

            if ("LMSR".equals(method)) {
                int b = Integer.parseInt(request.getParameter("b"));
                sm.createLmsrEvent(me, name, description, commissionRate, commissionType, opt1, opt2, b);
            } else if ("Order Book".equals(method)) {
                int initial = Integer.parseInt(request.getParameter("initial"));
                int d = Integer.parseInt(request.getParameter("d"));
                boolean allowMint = Boolean.parseBoolean(request.getParameter("allowMint"));
                sm.createOrderBookEvent(me, name, description, commissionRate, commissionType, opt1, opt2, initial, allowMint, d);
            } else {
                throw new Exception("Unknown trading method.");
            }
            ApiUtils.write(response, HttpServletResponse.SC_OK, "Event created.");

        } catch ( NullPointerException | IllegalArgumentException e) {
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing event parameters.");
        } catch (Exception e) {
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
}
