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

@WebServlet(urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String username = request.getParameter("username");

        if (username == null || username.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson("Error: Username cannot be empty."));
            return;
        }

        username = username.trim();
        SystemManager systemManager = (SystemManager) getServletContext().getAttribute("systemManager");

        synchronized (systemManager) {
            if (systemManager.getUsers().stream().anyMatch(u -> u.getName().equalsIgnoreCase(username))) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().write(gson.toJson("Username '" + username + "' is already taken. Please choose another."));
            } else {
                User newUser = new User(username);
                systemManager.addUser(newUser);
                request.getSession(true).setAttribute("loggedInUser", username);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(gson.toJson("Login successful"));
            }
        }
    }
}