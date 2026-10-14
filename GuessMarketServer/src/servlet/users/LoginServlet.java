package servlet.users;

import com.maneger.SystemManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.util.ApiUtils;

import java.io.IOException;

@WebServlet(urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        if (username == null || username.trim().isEmpty()) {
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, "Error: Username cannot be empty.");
            return;
        }
        username = username.trim();

        try {
            SystemManager sm = ApiUtils.manager(request);
            sm.registerUser(username);                       // atomic: no check-then-add race
            request.getSession(true).setAttribute(ApiUtils.SESSION_USER, username);
            ApiUtils.write(response, HttpServletResponse.SC_OK, "Login successful");
        } catch (Exception e) {
            ApiUtils.error(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        }
    }
}
