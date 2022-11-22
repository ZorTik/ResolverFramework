package me.zort.commons.resolverframework_dev;

import ez.DB;

public final class Resolver {

    public static ResolverService link(String packageSpace) {
        return me.zort.commons.resolverframework.Resolver.builder(packageSpace)
                .link();
    }

    public static ResolverService link(String packageSpace, MySQLCredentials mySQLCredentials) {
        String hostname = mySQLCredentials.getHost();
        String database = mySQLCredentials.getDatabase();
        String username = mySQLCredentials.getUser();
        String password = mySQLCredentials.getPassword();
        return me.zort.commons.resolverframework.Resolver.builder(packageSpace)
                .withMySQL(new DB(hostname, username, password, database))
                .link();
    }

}
