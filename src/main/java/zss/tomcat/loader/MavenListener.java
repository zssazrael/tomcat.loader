package zss.tomcat.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class MavenListener implements LifecycleListener {
    private static final Log LOGGER = LogFactory.getLog(MavenListener.class);

    private String mvn;

    public void setMVN(String mvn) {
        this.mvn = mvn;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (!"configure_start".equals(event.getType())) {
            return;
        }
        final Context context = getContext(event);
        if (context == null) {
            return;
        }
        final WebappClassLoaderBase classLoader = getClassLoader();
        if (classLoader == null) {
            return;
        }
        final File mvn = new File(this.mvn);
        if (!mvn.isFile()) {
            return;
        }
        final File base = new File(context.getDocBase());
        final File projectFolder = base.getParentFile().getParentFile().getParentFile();
        final File pom = new File(projectFolder, "pom.xml");
        if (!pom.isFile()) {
            return;
        }
        final Method addURL = getAddURL();
        final File classFolder = new File(projectFolder, "target/classes");
        try {
            addURL(classLoader, addURL, classFolder.toURI().toURL());
            final Process process = new ProcessBuilder(mvn.getAbsolutePath(), "-DincludeScope=runtime", "-o", "-f", pom.getAbsolutePath(), "dependency:build-classpath").start();
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("[")) {
                        continue;
                    }
                    for (String path : line.split(File.pathSeparator)) {
                        if (path.length() > 0) {
                            addURL(classLoader, addURL, new File(path).toURI().toURL());
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void addURL(final WebappClassLoaderBase classLoader, final Method addURL, final URL url) {
        if (classLoader != null && addURL != null && url != null) {
            try {
                addURL.invoke(classLoader, url);
            } catch (IllegalAccessException e) {
                LOGGER.error(e.getMessage(), e);
            } catch (IllegalArgumentException e) {
                LOGGER.error(e.getMessage(), e);
            } catch (InvocationTargetException e) {
                LOGGER.error(e.getMessage(), e);
            }
            LOGGER.info(String.format("addURL[%s]", url.toString()));
        }
    }

    private Method getAddURL() {
        try {
            final Method method = WebappClassLoaderBase.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        } catch (SecurityException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    private WebappClassLoaderBase getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        while (classLoader != null) {
            if (classLoader instanceof WebappClassLoaderBase) {
                return (WebappClassLoaderBase) classLoader;
            }
            classLoader = classLoader.getParent();
        }
        return null;
    }

    private Context getContext(LifecycleEvent event) {
        final Object source = event.getSource();
        if (source instanceof Context) {
            return (Context) source;
        }
        return null;
    }
}
