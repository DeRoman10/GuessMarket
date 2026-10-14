package servlet.users;

import com.data.users.entities.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.util.ApiUtils;

import java.io.IOException;

@WebServlet(urlPatterns = "/deposit")
public class DepositServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User me = ApiUtils.requireUser(request, response);
        if (me == null) return;

        try {
            double amount = Double.parseDouble(request.getParameter("amount"));
            ApiUtils.manager(request).depositFunds(me, amount);
            ApiUtils.write(response, HttpServletResponse.SC_OK, "Funds deposited successfully.");
        } catch (NumberFormatException | NullPointerException e) {
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, "Amount must be a valid number.");
        } catch (Exception e) {
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
}
