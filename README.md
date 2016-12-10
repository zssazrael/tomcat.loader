# tomcat.loader
开发 Maven Web 项目过程中，直接部署到tomcat，自动查找 jar，引用classes，而不需要用 maven package、
例子：
<Context docBase="xxxx\src\main\webapp" path="/manager" reloadable="false">
    <Loader className="zss.tomcat.loader.MavenWebappLoader" localRepository="xxxx\~m2"/>
</Context>
