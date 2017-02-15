package com.moparisthebest.phonehash.webservices;

import com.moparisthebest.filelist.LongConverter40Bit;
import com.moparisthebest.filelist.RandomAccessFileList;
import com.moparisthebest.phonehash.PhoneComparator;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.http.server.StaticHttpHandlerBase;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;

/**
 * Created by mopar on 9/12/14.
 */
@ApplicationPath("rest")
@Path("")
public class WebApp extends ResourceConfig {

    private static File sortedPhoneNumberList;
    private static final Pattern sha1regex = Pattern.compile("^[0-9a-fA-F]{40}$");

    @GET
    @Path("hashToPhone/{sha1}")
    @Produces(MediaType.TEXT_PLAIN)
    public String hashToPhone(@PathParam("sha1") final String sha1) throws IOException {
        if (sha1 == null || sha1.length() != 40 || !sha1regex.matcher(sha1).matches())
            return "sha1 hash must be 40 character hexadecimal";
        final PhoneComparator pc = new PhoneComparator();
        final byte[] needle = hexToBytes(sha1);
        try (RandomAccessFileList<Long> list = new RandomAccessFileList<>(sortedPhoneNumberList, LongConverter40Bit.instance)) {
            final long index = list.indexedBinarySearch(needle, pc);
            return index < 0 ? "Not found" : PhoneComparator.formatPhoneNumber(list.get(index));
        }
    }

    private static byte[] hexToBytes(final String hex) {
        final int len = hex.length();
        final byte[] array = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            array[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return array;
    }

    public WebApp() {
        packages(this.getClass().getPackage().getName());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.err.println("WebApp sortedPhoneNumbers.list [http://localhost:8080/phonehash/]");
            return;
        }
        sortedPhoneNumberList = new File(args[0]);
        String contextPath = args.length > 1 && !args[1].trim().isEmpty() ? args[1] : "http://localhost:8080/phonehash/";

        if (!contextPath.endsWith("/"))
            contextPath += "/";

        final ResourceConfig rc = new WebApp();

        final ApplicationPath ap = rc.getClass().getAnnotation(ApplicationPath.class);

        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(ap == null ? contextPath : contextPath + ap.value() + "/"), rc, false);

        final File webapp = new File("./src/main/webapp/");
        final StaticHttpHandlerBase staticHttpHandler;

        if (new File(webapp, "index.html").canRead()) {
            //staticHttpHandler = new CLStaticHttpHandler(new URLClassLoader(new URL[]{webapp.toURI().toURL()}));
            staticHttpHandler = new StaticHttpHandler(webapp.getAbsolutePath());
            staticHttpHandler.setFileCacheEnabled(false); // don't cache files, because we are in development?
            System.out.println("File Caching disabled!");
        } else {
            staticHttpHandler = new CLStaticHttpHandler(WebApp.class.getClassLoader()); // jar class loader, leave cache enabled
        }

        server.getServerConfiguration().addHttpHandler(staticHttpHandler,
                contextPath.replaceFirst("^[^/]+//[^/]+", "")
        );

        try {
            server.start();
            System.out.printf("Application started on '%s'.\nHit enter to stop it...", contextPath);
            System.in.read();
        } finally {
            server.shutdownNow();
        }
    }
}
