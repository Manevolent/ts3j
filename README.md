# ts3j

TS3J is an open-source implementation of the reverse-engineered Teamspeak3 full server/client protocol, as an adaptation of Splamy's C# TS3Client source code.  You can find that here: https://github.com/Splamy/TS3AudioBot/.

A standalone proof-of-concept was created to wrap ts3j: https://github.com/Manevolent/ts3j-musicbot

# A note about Teamspeak 5

I don't believe TS3j will need any major adjustments to work in a future Teamspeak5 world, based on what I have been able to get my hands on with TS5.  I've verified that TS3j does in fact work alongside TS5 clients, all connected to a "TS3" server.  There may be missing features that TS5 brings to the table, but those can be solved through issues and enhancement requests as they are identified.

# Projects using TS3j

TS3j has gotten a decent amount of attention for helping Java developers directly interact with Teamspeak3 servers and pipe audio for music bots, etc.  Maybe you're looking to build your idea on top of TS3j, but it's already been shared open-source.  That could save you a bunch of time!

| Name                     | GitHub                                    | Description                                                                                                                                                                                                                                                                                                                                     |
|--------------------------|-------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| JeakBot Plugin Framework | https://github.com/jeakfrw/jeak-framework | The JeakBot-Framework connects to the TeamSpeak server using the TS3 sever query interface.  Java plugins can use the API to interact with the TeamSpeak server using the JeakBot-API.  Plugins can be programmed in a way that developers may be familiar from the Sponge plugin API for Minecraft as the projects idea is inspired by Sponge. |
| Manebot                  | https://github.com/Manevolent/manebot-ts3 | The reference implementation of the Teamspeak3 platform for Manebot, a multi-platform chatbot framework.  You can use this plugin to get Manebot to interact with your Teamspeak3 server(s).                                                                                                                                                    |

If you made a project with TS3j on GitHub and would like to help people find it, feel free to let me know/open an issue and I'll add it here for you.


# Maven

If you want the latest `-SNAPSHOT`:

```xml
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

# Connection & Basic Setup

```java
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

// Subscribe to all channels
client.subscribeAll();

// Get list of clients
for (Client basicClient : client.listClients())
	recognizeClient(client.getClientInfo(basicClient.getId()));
```


Note that while `connect()` is processing, you'll receive channels registered and clients currently connected to the server.  It is important that you use the listener to collect these, and track their changes through the other listener event calls.

You can interact with the server using the commands on the `client` object similarly to TS3Query.

# Handling chat

```java
// TS3Listener interface
@Override
public void onTextMessage(TextMessageEvent textMessageEvent) {
	if (textMessageEvent.getInvokerId() == client.getClientId())
    		return;

	// Global chat example
	client.sendServerMessage("Echo!");
	
	// PM to the sender
	client.sendPrivateMessage(textMessageEvent.getInvokerId(), "Echo!");
}
```

# Sending audio

```java
// Microphone interface
public void write(float[] buffer, int len) {
	byte[] opusPacket = doOpusEncodingHere(buffer, len);

	packetQueue.add(opusPacket);
}

@Override
public boolean isReady() {
	return true;
}

@Override
public CodecType getCodec() {
	return CodecType.OPUS_MUSIC;
}

@Override
public byte[] provide() {
	try {
	    if (packetQueue.peek() == null)
		return new byte[0]; // underflow

	    OpusPacket packet = packetQueue.remove();

	    if (packet == null)
		return new byte[0]; // underflow

	    return packet.getBytes();
	} catch (NoSuchElementException ex) {
	    return new byte[0]; // signals the decoder on the clients to stop
	}
}
```
# Receiving audio

Refer to the `setVoiceHandler` and `setWhisperHandler` methods to supply a Consumer object to receive Voice and Whisper packets.  You will need to decode the packets yourself, and insert packet-loss-correction as needed.

Note that the first 5 packets starting a voice session are marked with the COMPRESSED flag.  The final voice packet, intended to singal to close your decoder and flush samples, is always empty (0-length byte array).

Manebot can do this in its TS3 plugin, which uses TS3j: https://github.com/Manevolent/manebot-ts3/tree/master/src/main/java/io/manebot/plugin/ts3/platform/audio/voice
