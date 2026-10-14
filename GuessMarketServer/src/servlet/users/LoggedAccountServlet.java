package servlet.users;

import com.data.users.dto.UserOwnPrivateData;
import com.data.users.entities.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.util.ApiUtils;

import java.io.IOException;

@WebServlet(urlPatterns = "/userHomePage")
public class LoggedAccountServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User me = ApiUtils.requireUser(request, response);
        if (me == null) return;

        UserOwnPrivateData profile;
        synchronized (ApiUtils.manager(request)) {      // same monitor the engine uses while it changes balances
            profile = UserOwnPrivateData.of(me);
        }
        ApiUtils.write(response, HttpServletResponse.SC_OK, profile);
    }
}
