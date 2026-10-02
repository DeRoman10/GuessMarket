package servlet.users;

import com.maneger.SystemManager;
import com.data.users.entities.User;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = "/deposit")
public class DepositServlet extends HttpServlet {

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

        String amountStr = request.getParameter("amount");

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                throw new Exception("Deposit amount must be strictly positive.");
            }

            synchronized (systemManager) {
                User user = systemManager.getUsers().stream()
                        .filter(u -> u.getName().equalsIgnoreCase(loggedInUsername))
                        .findFirst()
                        .orElseThrow(() -> new Exception("User not found."));

                user.deposit(amount);
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson("Funds deposited successfully."));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(e.getMessage()));
        }
    }
}