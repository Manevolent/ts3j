# ts3j
TS3J is an open-source implementation of the reverse-engineered Teamspeak3 full server/client protocol, as an adaptation of Splamy's C# TS3Client source code.  You can find that here: https://github.com/Splamy/TS3AudioBot/.

The aim of this project is to provide a full client, capable of performing all functions a full client can.  This project will be "headless" is not intended to be a bot or server itself; it only aims to be an API to interact with the client or server sockets.

If you are familiar with the Java Teamspeak3 serverQuery API, you will hit the ground running with this API as I am using several of their objects for the query side of the application.  I will also bring in as many commands as I can, meaning with a little bit of elbow grease you can move from a serverQuery bot to a TS3J bot more easily than otherwise.  Reason being, my current audio bot used serverQuery.

TS3J is formatted and stubbed for server support, and no logic has been written.  I've discovered that, with the protocol reversed, it may be possible to make a server that doesn't adhere to the licensing restrictions.  While nobody can stop anyone from making a reverse-engineered server as well now, I won't be sharing any code for one.

# Current Status

It should work, I'm using it on my bot
