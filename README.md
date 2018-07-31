# ts3j
TS3J is an open-source implementation of the reverse-engineered Teamspeak3 full server/client protocol, as an adaptation of Splamy's C# TS3Client source code.  You can find that here: https://github.com/Splamy/TS3AudioBot/.

The aim of this project is to provide a full client, capable of performing all functions a full client can.  This project will be "headless" is not intended to be a bot or server itself; it only aims to be an API to interact with the client or server sockets.

TS3J is formatted and stubbed for server support, and no logic has been written.  I've discovered that, with the protocol reversed, it may be possible to make a server that doesn't adhere to the licensing restrictions.  While nobody can stop anyone from making a reverse-engineered server as well now, I won't be sharing any code for one.

This project is currently not completed, but is being actively worked on until it is stable and working.  I'm very open to suggestions and contributions should you want to make any, so we can make this a reality faster.

I'll add more documentation on the API itself as we near a usable, stable client.
