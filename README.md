
# Lab. 1: Message Broker



## Theoretical background

### What is messaging?


[Enterprise Integration Patterns by Gregor Hohpe and Bobby Woolf, p. 13]

Messaging is a technology that enables high-speed, asynchronous, program-to-program communication with reliable delivery. Programs communicate by sending packets of data called messages to each other. Channels, also known as queues, are logical pathways that connect the programs and convey messages. A channel behaves like a collection or array of messages, but one that is magically shared across multiple computers and can be used concurrently by multiple applications. A sender or producer is a program that sends a message by writing the message to a channel. A receiver or consumer is a program that receives a message by reading (and deleting) it from a channel.

The message itself is simply some sort of data structure—such as a string, a byte array, a record, or an object. It can be interpreted simply as data, as the description of a command to be invoked on the receiver, or as the description of an event that occurred in the sender.




### What is brokering and what do we need it for?

In a distributed environment we usually deal with a large number of message senders and receivers which are part of the same system. The management of all message channels between these senders and receivers quickly becomes very difficult and transforms in a so called 'integration spagetti' because every sender has to know the address of the respective receiver or its message channel name. This has to do with the location transparency [George Coulouris - Distributed systems.. , p. 23] and specifically with its absence. Location transparency enables resources to be accessed without knowledge of their physical or network location (for example, which building or IP address).

To overcome an integration setup bloated with point-to-point communication and to enable the location transparency we can use the [Message Broker pattern](https://msdn.microsoft.com/en-us/library/ff648849.aspx).
A message broker is a physical component that handles the communication between applications. Instead of communicating with each other, applications communicate only with the message broker. An application sends a message to the message broker, providing the logical name of the receivers. The message broker looks up applications registered under the logical name and then passes the message to them.


### Pub/Sub and Routing

The 'broker' implementation presented in this laboratory work encompasses the concepts of the publish/subscribe pattern and content-based routing. The combination of these can be summarized under the name of [Content-Based Publish/Subscribe](https://msdn.microsoft.com/en-us/library/ff649664.aspx).

A [Publish-Subscribe Channel][Enterprise Integration Patterns by Gregor Hohpe and Bobby Woolf, p. 113] works like this: It has one input channel that splits into multiple output channels, one for each subscriber. When an event is published into the channel, the Publish-Subscribe Channel delivers a copy of the message to each of the output channels. Each output channel has only one subscriber, which is only allowed to consume a message once. In this way, each subscriber only gets the message once and consumed copies disappear from their channels.

The [Content-Based Router][Enterprise Integration Patterns by Gregor Hohpe and Bobby Woolf, p. 211] examines the message content and routes the message onto a different channel based on data contained in the message. The routing can be based on a number of criteria such as existence of fields, specific field values etc.



## Technical details and implementation decisions


### Project layout

The project represents a Gradle build with 4 subprojects. [Gradle](https://docs.gradle.org/current/userguide/introduction.html) is a build system for projects targeting the JVM platform. Its build scripts use a DSL based on Groovy programming language. It is also very flexible thanks to a large set of available plugins that include build tasks and scripts for various use cases. In the case of this laboratory work the [Java](https://docs.gradle.org/current/userguide/java_plugin.html) and [Application](https://docs.gradle.org/current/userguide/application_plugin.html) plugins are used. These plugins add tasks for building Java applications as well as running them as executables. At the end of the build process are obtained JAR files that can be deployed to any environment runing a Java Virtual Machine.


The 4 subprojects included in the solution are the following:

- sender - the entity that sends out messages.
- receiver - the entity that subscribes to a certain type of messages and expects them
- broker - the middleware used to route the messages to subscribers
- common - a set of classes that are shared among two or more subprojects



### Running the project and operation concept

As the result of the build process all three executable applications are packaged in self-contained .zip and .tar.gz archives that include the necessary .jar files as well as generated run-scripts (a bash script for *nix systems and a batch file to be run on Windows). These script can be passed various command-line arguments. In development, however, it is inconvenient to unpack every time the bundled .zip files. A solution for this is provided by Gradle. The `gradle run` command builds and runs the Java class specified as 'main' in the build script. By default the `gradle run` command is quite limited because it does not redirect any command line arguments to the application that is running. It also ignores the user input which comes through `stdin`. To overcome these issues it is necessary to customize the `run` task:


```groovy
project(':sender') {
    run {
        standardInput = System.in
        if(project.hasProperty('args')) {
            args project.args.split()
        }
    }
}
```

The code above tells Gradle to redirect whatever comes in `System.in` to the application which is running at the moment. It also specifies that if `gradle run` is provided with a `-Pargs="..."` flag, the string which is passed in should be split by spaces and resulting array should be fed to the app as a list of command line arguments.

Given the described setup, the `broker` should be started first, it operates on port `3000` which is hardcoded in its source code thus it does not require any command line arguments.

```
cd broker/
gradle run
```

After that it is possible to start a couple of instances of `receivers`. They should be told on which port to start, on what address and port is the broker located and what kind of messages should the given receiver be subscribed to. This information can be passed using CLI arguments.

```
cd receiver/
gradle run -Pargs="127.0.0.1 3000 4001 a b c"
```

The arguments meaning is as follows:
- `127.0.0.1` - the IP address of the broker
- `3000` - the Port on which the broker operates
- `4001` - the Port to which the current receiver should be bound
- `a b c` - a list of strings that represent the 'message types' in which this receiver is interested, for the sake of this laboratory work the message types are represented as lowercase latin letters.

At startup the `receiver` sends a special `subscribe` message to the `broker`. It includes the message types to which the receiver is to be subscribed. After that the broker will route all matching messages to the given receiver.

Now that the broker and receivers are working, a `sender` can be deployed as well using the following commands:

```
cd sender/
gradle run -Pargs="127.0.0.1 3000"
```

All the sender should know is the address and port of the broker. This already illustrates how decoupled is the sender from the receiver which is one of the main points of such an organisation of the system.

When started, the sender enters an infinite loop in which it asks the user to supply a message type and a message payload over and over again. The messages are then sent to the broker which consequently inspects the 'type' field of the message and dispatches it to the respective receivers.



### The Message model

Every message that is sent in the system is represented as an instance of the Message class which is located in the 'common' project and is shared among all three applications. The class has two fields `type` and `payload`. These fields are both of type 'String' in order to keep the whole project simple. All messages are XML serializable thanks to the annotations provided in the [JAXB (Java Architecture for XML Binding)](https://jaxb.java.net/tutorial/section_6_2_1-A-Survey-Of-JAXB-Annotations.html) library.

```java
@XmlRootElement
public class Message {

    /* ... methods for serialization and deserialization ... */

    @XmlElement
    public String type;
    @XmlElement
    public String payload;
}

```


### Transport protocol

To transmit messages over the network, the whole system makes use of [UDP (User Datagram Protocol)](https://www.ietf.org/rfc/rfc768.txt). Due to the fact that the system does not feature stream-like transmission of data and respectively does not require control over the message delivery order, the choice of UDP came naturally. UDP is a very light-weight protocol which does not require a live connection between nodes and maps maps gracefully on the messaging model used in the system. The absence of a connection makes connectivity failures transparent to the user which is characteristic to distributed systems.



### IO Abstraction

On of the goals of this laboratory work was exercising code reuse and programming to an interface rather than to an implementation. This can be observed in the organisation of the code responsible of input/output operations and specifically, how network and file operations are abstracted in ca common interface that has just two operations: `read` and `write`.

```java
public interface IIO {
    public void write(String s) throws IOException ;
    public String read() throws IOException ;
}
```

Such an abstraction, theoretically, provides the opportunity to exchange all network operations to file equivalents or other stream-like means of communication, given a very small amount of modifications in the code.



### Asynchrony

An environment distributed over the network is not possible without asynchronous communication. In this laboratory work, asynchronous operations are implemented using Java [Threads](https://docs.oracle.com/javase/tutorial/essential/concurrency/runthread.html) and [Executor Services](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html)



### Code flow


### Concurrent collections


## Conclusion


