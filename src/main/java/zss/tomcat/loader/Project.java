package zss.tomcat.loader;

public class Project
{
    private final ProjectList dependencies = new ProjectList();
    private final ProjectList exclusions = new ProjectList();

    public ProjectList getExclusions()
    {
        return exclusions;
    }

    private final String groupID;
    private final String artifactID;
    private final Project parent;
    private String version;
    private String scope;
    private String optional;

    public String getOptional()
    {
        return optional;
    }

    public void setOptional(final String optional)
    {
        this.optional = optional;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope(String scope)
    {
        this.scope = scope;
    }

    public Project getRoot()
    {
        Project root = this;
        while (root.parent != null)
        {
            root = root.parent;
        }
        return root;
    }

    public Project find(final String groupID, final String artifactID)
    {
        if (this.groupID.equals(groupID) && this.artifactID.equals(artifactID))
        {
            return this;
        }
        for (Project dependency : dependencies)
        {
            Project find = dependency.find(groupID, artifactID);
            if (find != null)
            {
                return find;
            }
        }
        return null;
    }

    public ProjectList getDependencies()
    {
        return dependencies;
    }

    public Project getParent()
    {
        return parent;
    }

    public String getGroupID()
    {
        return groupID;
    }

    public String getArtifactID()
    {
        return artifactID;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(final String version)
    {
        this.version = version;
    }

    public Project(final Project parent, final String groupID, final String artifactID)
    {
        this.groupID = groupID;
        this.artifactID = artifactID;
        this.parent = parent;
    }

    public void addDependency(final Project dependency)
    {
        dependencies.add(dependency);
    }

    public void addExclusion(final Project project)
    {
        exclusions.add(project);
    }

    public Project findExclusion(final String groupID, final String artifactID)
    {
        for (Project exclusion : exclusions)
        {
            if (exclusion.groupID.equals(groupID) && exclusion.artifactID.equals(artifactID))
            {
                return this;
            }
        }
        if (parent == null)
        {
            return null;
        }
        return parent.findExclusion(groupID, artifactID);
    }
}
