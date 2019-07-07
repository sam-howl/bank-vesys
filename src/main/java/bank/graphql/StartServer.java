package bank.graphql;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class StartServer {
    public static void main(String[] args) throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);

        String webappDirLocation = "src/main/webapp/";
        Context context = tomcat.addWebapp("/", new File(webappDirLocation).getAbsolutePath());

        String servletName = "Server";
        String urlPattern = "/graphql";

        Tomcat.addServlet(context, servletName, new Server());
        context.addServletMappingDecoded(urlPattern, servletName);

        tomcat.start();
        tomcat.getConnector();
        tomcat.getServer().await();
    }
}
