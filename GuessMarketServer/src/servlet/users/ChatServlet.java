package servlet.users;

import com.data.users.dto.ChatMessage;
import com.data.users.entities.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.util.ApiUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = "/chat")
public class ChatServlet extends HttpServlet {
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final List<ChatMessage> chatLog = Collections.synchronizedList(new ArrayList<>());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (ApiUtils.requireUser(request, response) == null) return;

        int since;
        try {
            since = Integer.parseInt(request.getParameter("since"));
        } catch (NumberFormatException e) {
            since = 0;
        }

        List<ChatMessage> fresh;
        synchronized (chatLog) {
            since = Math.max(0, Math.min(since, chatLog.size()));
            fresh = new ArrayList<>(chatLog.subList(since, chatLog.size()));
        }
        ApiUtils.write(response, HttpServletResponse.SC_OK, Map.of("messages", fresh, "next", since + fresh.size()));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User me = ApiUtils.requireUser(request, response);
        if (me == null) return;

        String text = request.getParameter("message");
        if (text == null || text.isBlank()) {
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST, "Message cannot be empty.");
            return;
        }
        if (text.length() > MAX_MESSAGE_LENGTH) {
            ApiUtils.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Message is too long (max " + MAX_MESSAGE_LENGTH + " characters).");
            return;
        }
        chatLog.add(new ChatMessage(me.getName(), text.trim(), System.currentTimeMillis()));
        ApiUtils.write(response, HttpServletResponse.SC_OK, "Sent.");
    }
}
