# rb-orm
Simple ORM for fun and games.

### Why:
I wanted to work on something that felt useful enough to use in other small projects, and explore a few common ORM design decisions that are normally invisible to or abstracted away from a user. At the same time, I didn't want to re-invent the wheel and implement the entire JPA. So this is a barebones ORM that does nothing fancy or special.

### How to Use:
1. Create the `SessionFactory` using one of the provided constructors...
   - `SessionFactory()` : creates and connects to an in-memory H2 database
   - `SessionFactory(Properties)` : creates a database connection as defined in a `java.util.Properties` object. Property names are listed in `com.rborm.context.PropertyNames` and below
   - `SessionFactory(File)` : creates a database connection as defined in a .properties file
   - `SessionFactory(String)` : creates a database connection as defined in a .properties file, to which the path is provided
2. Generate a `Session` object from the `SessionFactory`
3. If desired, begin a `Transaction`
4. Invoke any number of CRUD methods against the `Session`. If no `Transaction` is active, these will auto-commit.
  - The `Session` maintains a cache to any items retrieved from, or saved to, the database. 
  - To prefer routing `get()` and `delete()` operations to the cache, invoke `Session.useCache(boolean)` to route all subsequent operations 
5. If a `Transaction` was opened, `commit()` it or `rollback()` as desired

```java
SessionFactory sf = new SessionFactory();
Session session = sf.newSession();

Foo foo = new Foo(1);
session.useCache(true);
Transaction tx = session.beginTransaction();
session.save(foo);
tx.commit();

Foo bar = session.get(Foo.class, 1);
```

### Notes:
At this time, only PostgreSQL and H2 database connections are supported. There is also no support for connection properties that would otherwise be embedded in the connection URL

### Properties
```
rborm.database_engine     # h2 or postgres only
rborm.database_name
rborm.connection_url
rborm.connection_port
rborm.username
rborm.password
```
