package zss.tomcat.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.loader.VirtualWebappLoader;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class MavenWebappLoader extends VirtualWebappLoader {
    private static final Log LOGGER = LogFactory.getLog(MavenWebappLoader.class);

    private String mvn;

    public void setMVN(String mvn) {
        this.mvn = mvn;
    }

    public MavenWebappLoader() {
        super();
    }

    public MavenWebappLoader(final ClassLoader parent) {
        super(parent);
    }

    @Override
    protected void startInternal() throws LifecycleException {
        final File mvn = new File(this.mvn);
        if ((getContainer() instanceof Context) && mvn.isFile()) {
            final StringBuilder builder = new StringBuilder();
            try {
                final Context context = (Context) getContainer();
                if (context.getDocBase() != null) {
                    final File base = new File(context.getDocBase());
                    final File projectFolder = base.getParentFile().getParentFile().getParentFile();
                    final File pom = new File(projectFolder, "pom.xml");
                    final Process process = new ProcessBuilder(mvn.getAbsolutePath(), "-DincludeScope=runtime", "-o", "-f", pom.getAbsolutePath(), "dependency:build-classpath").start();
                    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("[")) {
                                continue;
                            }
                            for (String path : line.split(File.pathSeparator)) {
                                if (path.length() > 0) {
                                    builder.append(path).append(';');
                                }
                            }
                        }
                    }
                    final File classFolder = new File(projectFolder, "target/classes");
                    builder.append(classFolder.getAbsolutePath()).append(';');
                }
            } catch (IOException e) {
                throw new LifecycleException(e);
            }
            LOGGER.info(builder.toString());
            setVirtualClasspath(builder.toString());
        }
        super.startInternal();
    }
}
