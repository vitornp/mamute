package br.com.caelum.vraptor.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class Main {
	public static void main(String[] args) throws Exception {
		String webappDirLocation = getWebAppDir();
		//WatchService service = FileSystems.getDefault().newWatchService();
		
		String webXmlLocation = getWebXmlLocation(webappDirLocation);

		VRaptorServer vraptor = new VRaptorServer(webappDirLocation, webXmlLocation);
		vraptor.start();
		//configureWatcher(vraptor,	 webappDirLocation, service);

		//watchForChanges(service, vraptor);
	}

    private static String getWebXmlLocation(String webappDirLocation) {
        String webxml = System.getenv("VRAPTOR_WEBXML");
		webxml = webxml == null ? webappDirLocation + "/WEB-INF/web.xml" : webxml;
		return webxml;
    }

	private static void configureWatcher(VRaptorServer vraptor,
			String webappDirLocation, WatchService service) throws IOException {
		Path path = new File(webappDirLocation).toPath();
		path.register(service, StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_MODIFY);
	}

	@SuppressWarnings("unchecked")
	private static void watchForChanges(final WatchService watcher,
			final VRaptorServer server) {
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						WatchKey key = watcher.take();
						for (WatchEvent<?> event : key.pollEvents()) {
							WatchEvent<Path> ev = (WatchEvent<Path>) event;
							Path filename = ev.context();
							System.out.println(filename + " changed");
//							server.restartContexts();
						}
						key.reset();
					} catch (InterruptedException e) {
						System.out.println("Unable to detect change");
					}
				}
			}
		}).start();
	}

	private static String getWebAppDir() {
		return System.getProperty("vraptor.webappdir", "src/main/webapp/");
	}

}
