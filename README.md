# ManageQL: Run SQL against your JMX beans

Sometimes it's easier to to use SQL to access your JVM's management information that's stored in JMX.

## Building

    mvn clean install

## Running

There are two ways you can run the ManageQL server:

  1. Gateway mode: Starts up a new process running the ManageQL service which connects remotely to your
     JVM's JMX service.  This mode requires you to enable remote JMX access on that JVM.
  2. Agent mode: The ManageQL service is run in the same JVM where the JMX server lives.

The ManageQL server by default starts up a PostgreSQL compatible SQL protocol on port 5432.

### Running in Gateway Mode

First you need to make sure you enable remote connections into your JVM's JMX.  You can follow the
[this guide](https://docs.oracle.com/javase/tutorial/jmx/remote/jconsole.html) to set that up.
Assuming you configured the JVM to accept JMX connections on port 9999 on the localhost, you can then start the
ManageQL gateway like this:

    java -jar manageql-agent-<version>.jar gateway localhost:9999

### Running in Agent Mode

You can have your JVM always startup the ManageQL server by configuring the JVM to load MangeQL as an agent.
This is done by adding the following java command line arguments:

    -javaagent:manageql-agent-<version>.jar

You can also dynamically attach the agent to a running JVM even if it was not started with the `-javaagent` argument.
To do that use the `jps` command to get the `pid` for your java process.  Example usage:

    java -jar manageql-agent-<version>.jar start 1234

You can also to stop the ManageQL server running as an Agent (regardless of how it was started, at startup or attach)
by running the following stop command.

    java -jar manageql-agent-<version>.jar start 1234

### Connecting a SQL Client

You can use any PostgreSQL client to access the JMX data as SQL tables.  Example:

    $ psql -h localhost manageql
    Password for user chirino: 
    psql (11.2, server 8.2)
    Type "help" for help.
    
    manage=> select "$ObjectName" from "jmx.java.lang:*";
                          $ObjectName
    -------------------------------------------------------
     java.lang:type=OperatingSystem
     java.lang:type=MemoryManager,name=Metaspace Manager
     java.lang:type=MemoryPool,name=Metaspace
     java.lang:type=MemoryPool,name=PS Old Gen
     java.lang:type=ClassLoading
     java.lang:type=Runtime
     java.lang:type=GarbageCollector,name=PS Scavenge
     java.lang:type=Threading
     java.lang:type=MemoryManager,name=CodeCacheManager
     java.lang:type=MemoryPool,name=PS Eden Space
     java.lang:type=MemoryPool,name=Code Cache
     java.lang:type=MemoryPool,name=Compressed Class Space
     java.lang:type=MemoryPool,name=PS Survivor Space
     java.lang:type=GarbageCollector,name=PS MarkSweep
     java.lang:type=Memory
     java.lang:type=Compilation
    (16 rows)

    manage=> select Name,Type,UsageThresholdSupported from "jmx.java.lang:type=MemoryPool,*";
              Name          |   Type   | UsageThresholdSupported
    ------------------------+----------+-------------------------
     Compressed Class Space | NON_HEAP | true
     Metaspace              | NON_HEAP | true
     PS Old Gen             | HEAP     | true
     PS Eden Space          | HEAP     | false
     PS Survivor Space      | HEAP     | false
     Code Cache             | NON_HEAP | true
    (6 rows)

### The JMX SQL Schema

The ManageQL server defines a `manageql` database that contains a `jmx` schema.  The jmx schema
defines a table named to match the every MBean object name available in JMX.  Each attribute of
the MBean becomes a column of the table.  Note that since MBean names use special characters
you will need to quote the table name in your SQL statement.  Typical usage is:

    SELECT * FROM "jmx.<mbean-name>"

A synthetic "$ObjectName" column is also supported by all the jmx tables.  It will hold the
ObjectName of the MBean being selected.

You can also use table names that are ObjectName patterns that can match multiple MBeans.

The [ObjectName documentation](https://docs.oracle.com/javase/7/docs/api/javax/management/ObjectName.html) states:

> Examples of ObjectName patterns are:
> <ul>
> <li><code>*:type=Foo,name=Bar</code> to match names in any domain whose
>     exact set of keys is <code>type=Foo,name=Bar</code>.</li>
> <li><code>d:type=Foo,name=Bar,*</code> to match names in the domain
>     <code>d</code> that have the keys <code>type=Foo,name=Bar</code> plus
>     zero or more other keys.</li>
> <li><code>*:type=Foo,name=Bar,*</code> to match names in any domain
>     that has the keys <code>type=Foo,name=Bar</code> plus zero or
>     more other keys.</li>
> <li><code>d:type=F?o,name=Bar</code> will match e.g.
>     <code>d:type=Foo,name=Bar</code> and <code>d:type=Fro,name=Bar</code>.</li>
> <li><code>d:type=F*o,name=Bar</code> will match e.g.
>     <code>d:type=Fo,name=Bar</code> and <code>d:type=Frodo,name=Bar</code>.</li>
> <li><code>d:type=Foo,name="B*"</code> will match e.g.
>     <code>d:type=Foo,name="Bling"</code>. Wildcards are recognized even
>     inside quotes, and like other special characters can be escaped
>     with <code>\</code>.</li>
> </ul>
