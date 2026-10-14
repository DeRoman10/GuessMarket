package servlet.users;

import com.data.users.dto.AllUsersPublicData;
import com.data.users.entities.User;
import com.maneger.SystemManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.util.ApiUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** GET /users - the OTHER users: name, balance, is-MM. Nothing else about them leaves the server. */
@WebServlet(urlPatterns = "/users")
public class UsersServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User me = ApiUtils.requireUser(request, response);
        if (me == null) return;

        SystemManager sm = ApiUtils.manager(request);
        List<AllUsersPublicData> others = new ArrayList<>();
        synchronized (sm) {
            for (User u : sm.getUsers()) {
                others.add(AllUsersPublicData.of(u));
            }
        }
        others.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        ApiUtils.write(response, HttpServletResponse.SC_OK, others);
    }
}
