package zss.tomcat.loader;

import java.io.File;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        final ProjectTool projectTool = new ProjectTool(new File("D:\\maven"));
        final Project project = new Project(null, "", "");
        projectTool.load(project, new File("D:\\workspace\\manager\\pom.xml"));
        for (File file : projectTool.getDependencyList(project))
        {
            System.out.println(file);
        }
    }
}
