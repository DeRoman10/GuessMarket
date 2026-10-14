
package servlet.util;

import com.data.users.entities.User;
import com.data.events.entities.Event;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.maneger.SystemManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

public final class ApiUtils {
    public static final Gson GSON = new GsonBuilder()
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getDeclaringClass() == Event.class && "mmUser".equals(f.getName());
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .create();
    public static final String SESSION_USER = "loggedInUser";

    private ApiUtils() {}

    public static SystemManager manager(HttpServletRequest req) {
        return (SystemManager) req.getServletContext().getAttribute("systemManager");
    }

    public static void write(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(GSON.toJson(body));
    }

    public static void error(HttpServletResponse resp, int status, String message) throws IOException {
        write(resp, status, message);
    }


    public static User requireUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        String name = session == null ? null : (String) session.getAttribute(SESSION_USER);
        if (name != null) {
            try {
                return manager(req).getUser(name);
            } catch (Exception ignored) { /* fall through */ }
        }
        error(resp, HttpServletResponse.SC_UNAUTHORIZED, "Error: User not logged in.");
        return null;
    }
}

