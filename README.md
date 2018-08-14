# ts3j

Discord: https://discord.gg/4q9jpCa

Teamspeak: teamlixo.net (that's an SRV record; ts.teamlixo.net is the A record)

TS3J is an open-source implementation of the reverse-engineered Teamspeak3 full server/client protocol, as an adaptation of Splamy's C# TS3Client source code.  You can find that here: https://github.com/Splamy/TS3AudioBot/.

The aim of this project is to provide a full client, capable of performing all functions a full client can.  This project will be "headless" is not intended to be a bot or server itself; it only aims to be an API to interact with the client or server sockets.

If you are familiar with the Java Teamspeak3 serverQuery API, you will hit the ground running with this API as I am using several of their objects for the query side of the application.  I will also bring in as many commands as I can, meaning with a little bit of elbow grease you can move from a serverQuery bot to a TS3J bot more easily than otherwise.  Reason being, my current audio bot used serverQuery.

I recommend using tomp2p's opus-wrapper and encode 20ms slices of your PCM stream, in stereo, and provide the OPUS_MUSIC type through the Microphone class.  Ideally, you will encode packets on a separate thread, and place them into a Queue, where you will then dequeue them on the provide() method in Microphone.  This ensures the lowest possible jitter.

TS3J is formatted and stubbed for server support, and no logic has been written.  I've discovered that, with the protocol reversed, it may be possible to make a server that doesn't adhere to the licensing restrictions.  While nobody can stop anyone from making a reverse-engineered server as well now, I won't be sharing any code for one.

# Maven

If you want the latest `-SNAPSHOT`:

```
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
   <dependency>
	    <groupId>com.github.manevolent</groupId>
	    <artifactId>ts3j</artifactId>
	    <version>-SNAPSHOT</version>
	</dependency>
```

# Usage

```
client = new LocalTeamspeakClientSocket();

// Set up client
client.setIdentity(identity);
client.addListener(listener);
client.setNickname(nickname);

client.connect(
   new InetSocketAddress(
        InetAddress.getByName(address),
        port // UDP client port, Teamspeak3 client uses 9987
   ),
   password,
   10000L
);
```

Note that while `connect()` is processing, you'll receive channels registered and clients currently connected to the server.  It is important that you use the listener to collect these, and track their changes through the other listener event calls.

You can interact with the server using the commands on the `client` object similarly to TS3Query.
