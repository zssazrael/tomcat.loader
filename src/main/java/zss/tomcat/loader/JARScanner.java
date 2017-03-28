package zss.tomcat.loader;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.tomcat.JarScannerCallback;

public class JARScanner extends org.apache.tomcat.util.scan.StandardJarScanner {

    private String prefixion;

    public String getPrefixion() {
        return prefixion;
    }

    public void setPrefixion(String prefixion) {
        this.prefixion = prefixion;
    }

    @Override
    public void scan(ServletContext context, ClassLoader classloader, JarScannerCallback callback, Set<String> jarsToSkip) {
        ClassLoader loader = classloader;
        final Path prefixion = Paths.get(this.prefixion).toAbsolutePath().normalize();
        while (loader != null) {
            if (loader instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader) loader).getURLs()) {
                    final String text = url.toString();
                    if ("file".equalsIgnoreCase(url.getProtocol())) {
                        try {
                            if (Paths.get(url.toURI()).startsWith(prefixion)) {
                                callback.scan((JarURLConnection) URI.create("jar:".concat(text).concat("!/")).toURL().openConnection());
                            }
                        } catch (URISyntaxException e) {
                        } catch (MalformedURLException e) {
                        } catch (IOException e) {
                        }
                    }
                    if (text.endsWith("/target/classes/")) {
                        try {
                            callback.scan(new File(url.toURI()));
                        } catch (IOException e) {
                        } catch (URISyntaxException e) {
                        }
                    }
                }
            }
            loader = loader.getParent();
        }
        super.scan(context, classloader, callback, jarsToSkip);
    }
}
