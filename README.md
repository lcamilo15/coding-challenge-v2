# Number App Server


As specified by the requirements, this App
Number Server is a stand-alone application able to accept multiple socket
connections over TCP/IP for writing numbers in a single log file free from duplicates.

## System Requirements

In order to run Number Server on your machine you need:
* Java 11+ (how to install [here](https://www.oracle.com/java/technologies/javase-downloads.html))
* Gradle (how to install [here](https://gradle.org/install/)


### Building your project from the command line

To build the project on Linux or MacOS run the command `./gradlew build`
in a shell terminal.  This will build the source code in
`src/main/java`, run any tests in `src/test/java` and create an output
jar file in the `build/libs` folder.


## Running Application start

You can start the application using gradle :
```
 ./gradlew clean run
```

The server will start on port 400 and will create a numbers.log file located in the same path as the project. Note that the file will be cleared every time the server is restarted.

## Project Specification

- The maximum number of conection is 5.
- Every 10 seconds the application reports how many unique numbers have been tracked to the standard output:

* The difference since the last report of the count of new unique numbers that have been received.
* The difference since the last report of the count of new duplicate numbers that have been received.
* The total number of unique numbers received for this run of the
  Application.

An example of report could be the following:
```
Received 50 unique numbers, 2 duplicates. Unique total: 567231
```


## Simple app test
You can use Telnet to test conection to the server once the app is up.

Once the client session is created, a number can be sent by simply typing it and press enter.

ie:

```
telnet 127.0.0.1 4000
```

Any invalid number will cause the server to terminate the connection, valid input is as follows:

* A number can be composed by at most 9 digits (e.g. `314159265` or `007007009`);
* If a number is composed by less than 9 digits, it must include leading zeros till reach 9 digits;
* Any invalid sent number will disconnect the client;

* Typing `terminate` followed by enter will stop Number Server and disconnect all the clients.

## App Components


* `NumberServerApp` - Is the main application, it creates an instance of NumberServer and uses the NumberTrackerMonitor to track incoming messages. It also uses the validation defined in MessageUtils to validate incoming messages from the client.
* `NumberServer` - Contains information about the state of the server. It leverages a ThreadPoolExecutor with an empty queue and a max core/pool size equals to the max-connection it can handle. If we exceed the max number of threads a RejectedExecutionException is thrown and execution is thrown and conx is rejected; this happens when we reach max connection.
* `NumberServerWorkerFactory` -  factory used for creating Workers to handle client connection.
* `NumberServerWorker` - Workers used to handle communication between server and client, it allows for a convinient way to implement new server connections. Worker is the part that binds the listener attached to the NumberServer ie:
```  
numberServer.onConnect((serverConnection)->{
    // You can send a message here to the client:
    serverConnection.sendMessage("Message");

    serverConnection.onIncomingMessage(message->{             
         //If you return here, it will continue to listen to messsage
         if ({continue}) return;

         // If you need to send message to client
         if ({sendMessage})
         serverConnection.sendMessage("Message");

         //If you need to terminate connection
         serverConnection.close();
     });

   });
```

* `NumberTracker` - Is a wrapper containing information about unique numbers tracked, it allows to take a snapshot at any given time and resets unique and duplicate counters back to zero. It is composed of a NumberTrackerLogger which is an interface that is called whenever there is a unique number. It is not required for NumberTrackerLogger to be threadsafe since NumberTracker is synchronized when unique numbers are sent to the server

* `NumberBufferedFileLogger` - It is a new implementation of NumberTrackerLogger. It uses a BufferedWritter instead of a FileWritter. There was a noticeable improvement once this was added.


* `NumberFileLogger` - First implementation of the NumberTrackerLogger, uses a simple FileWritter, this was kept so I can do test and compare it to the NumberBufferedFileLogger

## Development style

Number Server has been developed following as much as possible SOLID and TDD principles. It has a final
code coverage of about `87%`. The number of classes has been kept as much as possible close to the architectural design
for improving the readability.

## Technical decisions

* The code was broken into multiple classes to allow for easy testing of individual compoents
* The socket stream is read using a `java.io.BufferedReader` which proved better performance respect to
  `java.util.Scanner`;
* ~~The file log is written using `java.io.FileWriter` because of its performance~~;
* File log is written using BufferredWriter to improve performance.
* Numbers are logged leading zeroes.
* Mockito is used to emulate some of the behavior of the tests.

## NOTE:

There were 2 issues found during the code reviews:

1- Use of flush on every write:
- I noticed on one of my tests that the output was not being written into the file which is why I decided to use flush.

- See
  NumberServerAppTest.valid_digits_should_be_logged_into_file, the assertion was failing once I removed the flush, I fixed this by Exposing the flush from the numberBufferedFileLogger.

2- There was an error on the trackedNumbers not being accurate, This happened because I was expecting values to be available before thread had finished, I fixed this by using mockito to wait on a specific method to be called:

- See NumberServerAppTest.test_number_tracker


```
verify(numberTracker, timeout(5000).times(maxClients *3)).trackNumber(Mockito.anyInt());
```