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
import java.util.List;

@WebServlet(urlPatterns = "/users")
public class UsersServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        SystemManager systemManager = (SystemManager) getServletContext().getAttribute("systemManager");
        String loggedInUsername = (String) request.getSession().getAttribute("loggedInUser");

        if (loggedInUsername == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson("Error: User not logged in."));
            return;
        }

        try {
            synchronized (systemManager) {
                List<User> users = systemManager.getUsers();
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(gson.toJson(users));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson("Server Error: " + e.getMessage()));
        }
    }
}