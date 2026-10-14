package servlet.system;

import com.maneger.SystemManager;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class ServerInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        SystemManager systemManager = new SystemManager();
        sce.getServletContext().setAttribute("systemManager", systemManager);
        System.out.println("Guess Market Server Initialized successfully!");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        SystemManager systemManager = (SystemManager) sce.getServletContext().getAttribute("systemManager");
        if (systemManager != null && systemManager.getEngineService() != null) {
            systemManager.getEngineService().shutdown();
        }
    }
}

