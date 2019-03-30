# ManageQL: Run SQL against your JMX beans

Sometimes it's easier to to use SQL to access your JVM's management information that's stored in JMX.

## Building

    mvn clean install

## Running

Assuming you have a JVM thats has remote connections enabled on port 1099.  You can run the manageql server against that JMX port like this:

    java -jar manageql-agent/target/manageql-agent-*-SNAPSHOT.jar service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi


If you don't want to expose your JMX server remotely, then you can startup the ManageQL server as an
agent in your JVM by using the following java arguments:

    -javaagent:manageql-agent/target/manageql-agent-*-SNAPSHOT.jar

This starts up a new PostgreSQL compatible SQL protocol on port 35432.  You can then connect to it with any PostgreSQL client.  For example:

    $ psql -h localhost -p 35432 manage 
    Password for user chirino: 
    psql (11.2, server 8.2)
    Type "help" for help.
    
    manage=> select "$ObjectName" from "jmx.org.apache.activemq:*";
                                                                                          $ObjectName                                                                                      
    ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Topic,destinationName=foo
     org.apache.activemq:type=Broker,brokerName=localhost,connector=clientConnectors,connectorName=stomp
     org.apache.activemq:type=Broker,brokerName=localhost,connector=clientConnectors,connectorName=openwire
     org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Topic,destinationName=TEST.FOO
     org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=TEST.FOO
     org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=foo
     org.apache.activemq:type=Broker,brokerName=localhost,service=Log4JConfiguration
     org.apache.activemq:type=Broker,brokerName=localhost
     org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Topic,destinationName=TEST.FOO,endpoint=Consumer,clientId=consumer1,consumerId=Durable(consumer1_James)
     org.apache.activemq:type=Broker,brokerName=localhost,connector=clientConnectors,connectorName=ws
     org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Topic,destinationName=ActiveMQ.Advisory.MasterBroker
     org.apache.activemq:type=Broker,brokerName=localhost,service=Health
     org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=incomingOrders
     org.apache.activemq:type=Broker,brokerName=localhost,service=PersistenceAdapter,instanceName=KahaDBPersistenceAdapter[/Users/chirino/Applications/apache-activemq-5.12.1/data/kahadb]
     org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=ActiveMQ.DLQ
     org.apache.activemq:type=Broker,brokerName=localhost,connector=clientConnectors,connectorName=mqtt
     org.apache.activemq:type=Broker,brokerName=localhost,connector=clientConnectors,connectorName=amqp
    (17 rows)

    manage=> select Name, DispatchCount from "jmx.org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Topic,*" where name is not null;
                  Name              | DispatchCount 
    --------------------------------+---------------
     foo                            | 0
     TEST.FOO                       | 0
     ActiveMQ.Advisory.MasterBroker | 0
    (3 rows)
