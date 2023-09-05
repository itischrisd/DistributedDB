# DistributedDB

This project is a distributed database application developed as a final project assignment for the SKJ (Computer Networks nad Java Network Programming) practical classes, taught by Piotr Smażyński during studies on [PJAIT](https://www.pja.edu.pl/en/).

## Project Description

The DistributedDB project aims to implement a distributed database system using a non-directed graph network structure. The system allows for the storage and retrieval of key-value pairs across multiple nodes, providing fault tolerance and scalability. The implementation includes a custom application layer protocol for exchanging messages between nodes and routing packets within the database. The main goal of the project was to address different aspects of network topics, which is reflected by the following techincal description.

## Allowed client commands

- `set-value <key>:<value>` : set a new value (second parameter) for the key being the first parameter. The result of the operation is the message `OK` if the operation is successful or `ERROR` if the database does not contain any nodes that contain the requested key. If there are several records with the same key in the database, all of them will change.
- `get-value <key>` : getting the value for the key which is a parameter in the database. The result of the operation is a message consisting of the pair `<key>:<value>` if the operation succeeded or `ERROR` if the database does not contain any nodes that contain the requested key. If the database contains more than one pair with such a key, only one result will be returned.
- `find-key <key>` : request to find the address and port number of the node in which a record with a given key is stored. If such a node exists, the answer is a pair `<address>:<port>` to identify this node, or `ERROR` if no node has such a key. If the database contains more than one pair with such a key, only one result will be returned.
- `get-max` : find the largest value assigned to all keys in the database. The result of the operation is a message consisting of the pair `<key>:<value>`. If base contains more than one pair with such a key, only one result will be returned.
- `get-min` : find the smallest value assigned to all keys in the database. The result of the operation is a message consisting of the pair `<key>:<value>`. If base contains more than one pair with such a key, only one result will be returned.
- `new-record <key>:<value>` : store a new key:value pair in place of the pair stored on the node to which the client is connected. The result of this operation is `OK` message.
- `terminate` : causes the node to disconnect from the network, informing its neighbours about the fact. Node neighbours informed about the termination stop communication to that address. Terminated node sends an `OK` message to the client.


## Protocol - Packet Structure

To understand the implementation of the distributed database project, it's important to describe the protocol used for communication within the database. The protocol consists of two types of messages: simple messages and complex messages.

1. Simple Messages: These messages are identical to the ones sent by the client and are used for accepting client requests, forwarding the final response from a gateway node to the client, and informing nodes about attaching/detaching new neighbours. The structure of a simple message, following the project requirements, is as follows:

```
<COMMAND FIELD> <REQUEST/RESPONSE BODY>
```

The fields are separated by spaces, and both fields are optional. If the message is a response to a client, it won't contain the command field (e.g., 'ERROR', 'OK', '127.0.0.1:9000'). If the message is a 'terminate' command, it won't contain the request body. As said before, simple messages are also used to communicate attachment statuses between nodes, in which case the ordering of the fields is inverted.

2. Complex Messages: These messages are used for propagating requests within the system and for transmitting responses. The structure of a complex message is as follows:

```
<ID> <COMMAND FIELD> <REQUEST BODY> <RESPONSE BODY> <PACKET HISTORY FIELD> <LAST SENDER FIELD>
```

Again, the fields are separated by spaces. They contain the following information:

- ID: This field uniquely identifies a packet associated with a specific client request within the system. It is assigned by the first node receiving the client request and is determined by the system time in seconds. The ID allows for quick differentiation between client requests and internal intra-node communication. Internal packets always start with a digit, while simple (client) packets always start with a letter. The ID field remains unchanged throughout the packet's life cycle and is used by nodes to identify which functions within their logic to execute when processing the request.
- Command Field: This field specifies the command associated with the request.
- Request Body: This field contains the parameters of the client request. If the client request has no parameters, the field is created with the value "NUL".
- Response Body: This field is used to provide the response to the client request. Initially, it is set to "ERROR" to indicate that the request couldn't be fulfilled. If a node can fulfill the request (e.g., by executing a 'set-value' command or having the response to a 'find-key x' command), it replaces the initial "ERROR" value with the appropriate data.
- Packet History Field: This field allows for optimal routing of the request packet. When the packet arrives at a node for the first time, it appends its IP:PORT address (separated by commas) to the end of this field. This ensures that each node processes the packet only once and knows which of its neighbors have already processed it. The addresses are concatenated without spaces, as specified in the protocol.
- Last Sender Field: Contains "IP:PORT" value of incoming socket of a node that has forwarded the packet to the current node. Used by the current node to later determine to whom forward the response by saing the value in local packet history.


## Network Topology Handling

The distributed database system supports the implementation of a network of database nodes represented by an arbitrary non-directed graph. The technical aspect of this implementation is as follows: when a node starts, it creates a ServerSocket object that listens on a specified port, as provided by the command-line parameter. The socket operates in blocking mode, continuously waiting for incoming TCP connections or TCP handshakes from other nodes/clients. Each node is in idle status, waiting for the ServerSocket object to return a new connection. If a new socket is created, the node remembers it as either a client connection for later response transmission, or saves an attached node if handshake data came from it. If a node receives information that a socket has been closed, it removes it from the list and will no longer forward packets to it. Tests have shown that the creation and closing of connections work for at least 20 nodes connected in non-trivial graph structures. Immediate handling of received client requests combined with identifying unique requests, fulfills the project requirement 3.4 for parallel handling of multiple clients connected to any border nodes (tests were performed with 7 nodes and 7 simultaneous client connections).

## Packet Routing

To fully explain how the distributed system routes request/response packets between its nodes, it's necessary to understand how packet data is stored. The first storage location is the packet's history field, which tracks its journey through the nodes. The second is the memory of each node. When a node receives a packet from a client or a neighboring node, it checks (based on the ID field) if it's the first time the packet related to the request arrives at the node or if it has visited the node before. For the first visit, the node adds the key-value pair to its memory, where the key is the ID taken from the packet and the value is the IP address of the original sender (the "original" sender is the one that first sent the packet to the node). The value is never overwritten and always points to the original sender's address. The first visit to a node is the only time the node executes the method specified by the command field, using the request body as an argument, and if necessary, it updates the response body field with the appropriate data.

The next step in packet routing is finding the first neighbor whose address is not in the packet's history field. If a node determines that all of its neighbors have already processed the packet (e.g., the sender is the only neighbor or the packet is on its return path to the client), it checks its memory to find the value associated with the key equal to the packet's ID. After obtaining the value, the node creates a corresponding socket and forwards the packet to that socket, assuming the request is already handled by its neighbors.

Before sending the processed packet "up the hierarchy," the node checks if it is the first address in the packet's history. If it is, it means that the client is the original sender. In this case, before forwarding the packet, the node converts the complex message to a simple message so that the client receives only the requested data.

With this routing explanation, and assuming that the network forming the distributed database is an arbitrary non-directed graph, it becomes clear that the routing is based on a custom Depth First Search algorithm. This implementation ensures that:
- Each request is handled by each node only once.
- The request reaches every node in the distributed database.
- The request packet is not duplicated or deadlocked anywhere in the network.
- The request is transparent for the node, if its only job is to forward the data in correct direction.
- The response packet finds its way back to the client without coordination between nodes, always returning through the same path it came from, while bypassing irrelevant branches in the graph.

## Packet Processing

As mentioned earlier, a node executes the logic for handling a request only during the first visit of the associated packet. Without going into the details of the Java implementation functions, the distribution of the processing for each command is as follows:
- set-value: The packet traverses the network in search of a node with the specified key. Until it finds a matching node, the response body of the packet contains the initial "ERROR" value. If the packet doesn't find a node with the correct key and returns to a gateway node, it will naturally transmit the "ERROR" value back to the client. If, along the way, one or more nodes are found to have the matching key, each node sets the value from the request body in its memory and updates the response body to "OK" before forwarding the packet. The packet reaches every node, and each node with the specified key will have the corresponding value set.
- get-value: Similar to the set-value command, the packet traverses the entire system, and each node with the matching key overwrites the response body field with its own key-value pair (overwriting "ERROR" if it was the first successful match or the previous key-value pair if subsequent matches occur).
- find-key: This command is implemented identically to the get-value command. The packet also traverses the entire system, and each node with the matching key overwrites the response body field with its own key-value pair. In this case, the packet returns the last IP:PORT address of the owner of the searched key.
- get-max: The request packet traverses the network, and each node compares its stored value with the one in the response body field. If the stored value is greater, it overwrites the response body field with its own key-value pair. If the node is the first in the packet's path, it always inserts its key-value pair in the response body field.
- get-min: Similar to the get-max command, but in this case, the node looks for the smallest value.
- new-record: This is the only client request that doesn't trigger packet forwarding within the internal routing algorithm. The node simply adds the key-value pair from the request body to its internal memory and sends an "OK" message back to the client.
- terminate: Upon receiving a "terminate" message from a client, a node sends an "OK" message to each of its sockets and closes the connections. This ensures that the client receives the expected response, and the neighboring nodes, upon receiving the same "OK" message, know that the sender has terminated and should close the connection from their side, removing it from their lists of established connections and neighbors.

## Compilation

The project can be compiled using the following command:

```
javac *.java
```

It is compatible with Java 1.8.

## Execution

Prepared test scenarios can be run from .bat files on Windows platform or .sh files on Linux/Unix. The execution of the project follows the requirements specification.

To run a node first execute following command:
```
java DatabaseNode -tcpport PORT [-connect HOST:PORT] -record KEY:VALUE
```
To run a client execute following command:
```
java DatabaseNode -gateway HOST:PORT -operation COMMAND [ADDITIONAL_PARAMETER]
```

## Known Issues

The author encountered an issue related to the address of the first node in the database. Each node, upon establishing a connection, checks its address based on the socket used for that connection with another existing node. The first node in the network does not create such a socket, so the author manually set the address of the first node to 127.0.0.1. Due to this limitation, the database may not function properly on nodes running on separate computers. Possible fix could be to update self-contained IP address each time the node receives incoming communication. No other issues were identified, although, following the 7th testing principle, "The belief that there are no bugs is itself a bug."

## License, disclaimers, known issues

The following code is distributed under the [GPLv3](./LICENSE).

---

If you need some help, notice any bugs or come up with possible improvements, feel free to reach out to me and/or create a pull request.
