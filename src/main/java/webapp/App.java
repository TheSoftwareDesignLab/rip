package webapp;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import java.awt.Desktop;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static helper.ExternalProcess2.executeProcess;


public class App {

	public App(String pPort, boolean openBrowser){

		//BasicConfigurator.configure();
		int port = 8080;
		if(pPort != null)
		{
			port = Integer.parseInt(pPort);
		}
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

		Server server = new Server(port);
		server.setHandler(context);
		//Static files management
		DefaultServlet defaultServlet = new DefaultServlet();
		ServletHolder staticHolder = new ServletHolder("default", defaultServlet);
		staticHolder.setInitParameter("resourceBase", "./src/webApp");

		context.addServlet(staticHolder, "/*");

		try {

			System.out.println("Server running on port "+port);
			server.start();

			System.out.println("System OS "+System.getProperty("os.name"));
			String os = System.getProperty("os.name");

			if(openBrowser)
			{
				String s = "http://localhost:"+port;

				//Guardar plataforma y poner switch case para alternativas de Desktop
				if (Desktop.isDesktopSupported()){
					Desktop desktop = Desktop.getDesktop();
					desktop.browse(URI.create(s));
				}
				else if(os.equals("Linux")) {
					List<String> commands = Arrays.asList("xdg-open", s);
					List<String> result = executeProcess(commands, "Open browser in linux", "Browser was opened",
							"There has been an error, please check if you have installed  xdg-open");
				}
			}

			server.join();

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		finally {
			try {
				server.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
			server.destroy();
		}
	}
}
