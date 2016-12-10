package zss.tomcat.loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.loader.WebappLoader;
import org.jdom.JDOMException;

public class MavenWebappLoader extends WebappLoader
{
    private String localRepository;

    public String getLocalRepository()
    {
        return localRepository;
    }

    public void setLocalRepository(final String localRepository)
    {
        this.localRepository = localRepository;
    }

    public MavenWebappLoader()
    {
        super();
    }

    public MavenWebappLoader(final ClassLoader parent)
    {
        super(parent);
    }

    @Override
    protected void stopInternal() throws LifecycleException
    {
        super.stopInternal();
    }

    @Override
    protected void startInternal() throws LifecycleException
    {
        if ((getContainer() instanceof Context) && (localRepository != null))
        {
            final Context context = (Context) getContainer();
            if (context.getDocBase() != null)
            {
                final File base = new File(context.getDocBase());
                final File projectFolder = base.getParentFile().getParentFile().getParentFile();
                final File pom = new File(projectFolder, "pom.xml");
                final Project project = new Project(null, "", "");
                final File folder = new File(localRepository).getAbsoluteFile();
                if (folder.isDirectory())
                {
                    final ProjectTool projectTool = new ProjectTool(folder);
                    try
                    {
                        projectTool.load(project, pom);
                    }
                    catch (FileNotFoundException e)
                    {
                        throw new LifecycleException(e);
                    }
                    catch (IOException e)
                    {
                        throw new LifecycleException(e);
                    }
                    catch (JDOMException e)
                    {
                        throw new LifecycleException(e);
                    }
                    for (File file : projectTool.getDependencyList(project))
                    {
                        addRepository(file.toURI().toString());
                    }
                }
                final File classFolder = new File(projectFolder, "target/classes");
                addRepository(classFolder.toURI().toString());
            }
        }
        super.startInternal();
    }
}
