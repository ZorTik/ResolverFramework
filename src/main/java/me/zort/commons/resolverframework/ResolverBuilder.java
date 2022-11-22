package me.zort.commons.resolverframework;

import ez.DB;

public class ResolverBuilder {

    private String packageSpace;

    private ClassLoader classLoader;
    private boolean debug;
    private DB mySQL;

    protected ResolverBuilder(String packageSpace) {
        this.packageSpace = packageSpace;
        this.classLoader = null;
        this.debug = false;
        this.mySQL = null;
    }

    public ResolverBuilder withClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public ResolverBuilder withMySQL(DB mySQL) {
        this.mySQL = mySQL;
        return this;
    }

    public ResolverBuilder withDebug() {
        this.debug = true;
        return this;
    }

    public ResolverService link() {
        ResolverService service = ResolverService.link(packageSpace, classLoader);
        service.setDebug(debug);
        if(mySQL != null) service.enableMysql(mySQL);
        return service;
    }

}
