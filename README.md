# ts3j
A open-source Java Teamspeak 3 client library using the TS3 full client protocol

This is a work in progress.  I am working from Splamy's TS3Client project.  This project is aiming to be a rewrite of
that codebase in Java.  I'm leaving the door open for future server support, if that's even possible (we don't know what
a lot of the handshake params are I don't think, and clients may require they be specific somehow).

Most of the code is written quite flexibly.

Client and server headers are abstract, and share a command PacketHeader class.  Packets mutate their input and output
behavior based on the assumed role of the packet.  Together, they transport over the wire in the form of a NetworkPacket
object.  This object can be re-constructed automatically through the use of a PacketType enum, meaning I/O is pretty
simple through the use of each packet's getters/setters and the read() and write() calls to NetworkPacket.

The client code is split into pieces: Socket and Client.  Since Teamspeak3 relies on a UDP socket, I decided to make
a base Taemspeak3Socket class which does the generic I/O ops which both the server and client should share in common.
Teamspeak3Client is a class which wraps the Teamspeak3Socket class and provides more client-oriented behavior and methods.
This setup is a state machine, so the client connection and connection setup logic is based on a series of handlers
which correspond to the state the client is in.