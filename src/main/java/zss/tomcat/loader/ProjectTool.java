package zss.tomcat.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class ProjectTool
{
    private static final Log LOGGER = LogFactory.getLog(ProjectTool.class);

    private final File folder;

    public File getFolder()
    {
        return folder;
    }

    public ProjectTool(final File folder)
    {
        this.folder = folder;
    }

    public void load(final Project project, final Document document) throws FileNotFoundException, IOException, JDOMException
    {
        final Element projectElement = document.getRootElement();
        loadModule(project, projectElement);
        final Project root = project.getRoot();

        for (Object dependenciesElementObject : projectElement.getChildren("dependencies", projectElement.getNamespace()))
        {
            final Element dependenciesElement = (Element) dependenciesElementObject;
            for (Object dependencyElementObject : dependenciesElement.getChildren("dependency", dependenciesElement.getNamespace()))
            {
                final Element dependencyElement = (Element) dependencyElementObject;
                final String dependencyGroupID = dependencyElement.getChildTextTrim("groupId", dependencyElement.getNamespace());
                final String dependencyArtifactID = dependencyElement.getChildTextTrim("artifactId", dependencyElement.getNamespace());
                if (root.find(dependencyGroupID, dependencyArtifactID) != null)
                {
                    continue;
                }
                if (project.findExclusion(dependencyGroupID, dependencyArtifactID) != null)
                {
                    continue;
                }
                final Project dependency = new Project(project, dependencyGroupID, dependencyArtifactID);
                final Element dependencyVersionElement = dependencyElement.getChild("version", dependencyElement.getNamespace());
                final Element dependencyScopeElement = dependencyElement.getChild("scope", dependencyElement.getNamespace());
                final Element dependencyOptionalElement = dependencyElement.getChild("optional", dependencyElement.getNamespace());
                if (dependencyVersionElement != null)
                {
                    dependency.setVersion(dependencyVersionElement.getTextTrim());
                }
                if (dependencyScopeElement != null)
                {
                    dependency.setScope(dependencyScopeElement.getTextTrim());
                }
                if (dependencyOptionalElement != null)
                {
                    dependency.setOptional(dependencyOptionalElement.getTextTrim());
                }
                if ("true".equals(dependency.getOptional()))
                {
                    continue;
                }
                if ("provided".equals(dependency.getScope()))
                {
                    continue;
                }
                if ("test".equals(dependency.getScope()))
                {
                    continue;
                }
                for (Object exclusionsElementObject : dependencyElement.getChildren("exclusions", dependencyElement.getNamespace()))
                {
                    final Element exclusionsElement = (Element) exclusionsElementObject;
                    for (Object exclusionElementObject : exclusionsElement.getChildren("exclusion", exclusionsElement.getNamespace()))
                    {
                        final Element exclusionElement = (Element) exclusionElementObject;
                        final String exclusionGroupID = exclusionElement.getChildTextTrim("groupId", exclusionElement.getNamespace());
                        final String exclusionArtifactID = exclusionElement.getChildTextTrim("artifactId", exclusionElement.getNamespace());
                        dependency.addExclusion(new Project(dependency, exclusionGroupID, exclusionArtifactID));
                    }
                }
                if (dependency.getVersion() == null)
                {
                    dependency.setVersion(project.getModuleMap().get(dependency.getArtifactID()));
                }
                project.addDependency(dependency);
            }
        }

        for (Project dependency : project.getDependencies())
        {
            if (dependency.getVersion() != null)
            {
                load(dependency);
            }
        }
    }

    private void loadModule(Project project, Element projectElement) throws JDOMException, IOException
    {
        final Element parentElement = projectElement.getChild("parent", projectElement.getNamespace());
        if (parentElement == null)
        {
            return;
        }

        final String parentGroupID = parentElement.getChildTextTrim("groupId", parentElement.getNamespace());
        final String parentArtifactID = parentElement.getChildTextTrim("artifactId", parentElement.getNamespace());
        final String parentVersion = parentElement.getChildTextTrim("version", parentElement.getNamespace());
        if ((parentVersion == null) || (parentArtifactID == null) || (parentGroupID == null))
        {
            return;
        }
        final File parentGroupFolder = new File(folder, parentGroupID.replace('.', '/'));
        final File parentArtifactFolder = new File(parentGroupFolder, parentArtifactID);
        final File parentVersionFolder = new File(parentArtifactFolder, parentVersion);
        final File pomFile = new File(parentVersionFolder, parentArtifactID.concat("-").concat(parentVersion).concat(".pom"));
        if (!pomFile.isFile())
        {
            return;
        }
        LOGGER.info(pomFile.getAbsolutePath());
        final Document parentDocument = new SAXBuilder(false).build(pomFile);
        final Element modulesElement = parentDocument.getRootElement().getChild("modules", parentDocument.getRootElement().getNamespace());
        if (modulesElement == null)
        {
            return;
        }
        for (Object moduleElementObject : modulesElement.getChildren("module", modulesElement.getNamespace()))
        {
            final Element moduleElement = (Element) moduleElementObject;
            project.getModuleMap().put(moduleElement.getTextTrim(), parentVersion);
        }
    }

    private void load(final Project dependency) throws FileNotFoundException, IOException, JDOMException
    {
        final File groupFolder = new File(folder, dependency.getGroupID().replace('.', '/'));
        final File artifactFolder = new File(groupFolder, dependency.getArtifactID());
        final File versionFolder = new File(artifactFolder, dependency.getVersion());
        final File pomFile = new File(versionFolder, dependency.getArtifactID().concat("-").concat(dependency.getVersion()).concat(".pom"));
        if (!pomFile.isFile())
        {
            return;
        }
        load(dependency, pomFile);
    }

    public void load(final Project dependency, final File pom) throws FileNotFoundException, IOException, JDOMException
    {
        LOGGER.info(pom.getAbsolutePath());
        try (final InputStream stream = new FileInputStream(pom))
        {
            final SAXBuilder builder = new SAXBuilder(false);
            load(dependency, builder.build(stream));
        }
    }

    public List<File> getDependencyList(final Project project)
    {
        final List<File> list = new LinkedList<>();
        for (Project dependency : project.getDependencies())
        {
            final File groupFolder = new File(folder, dependency.getGroupID().replace('.', '/'));
            final File artifactFolder = new File(groupFolder, dependency.getArtifactID());
            if (dependency.getVersion() == null)
            {
                continue;
            }
            final File versionFolder = new File(artifactFolder, dependency.getVersion());
            final File jarFile = new File(versionFolder, dependency.getArtifactID().concat("-").concat(dependency.getVersion()).concat(".jar"));
            if (jarFile.isFile())
            {
                list.add(jarFile);
                list.addAll(getDependencyList(dependency));
            }
        }
        return list;
    }
}
