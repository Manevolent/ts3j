package com.github.manevolent.ts3j.protocol.packet.handler.local;

import com.github.manevolent.ts3j.command.CommandHandler;
import com.github.manevolent.ts3j.command.SingleCommand;
import com.github.manevolent.ts3j.command.parameter.CommandSingleParameter;
import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.packet.PacketBody2Command;
import com.github.manevolent.ts3j.protocol.packet.PacketBody8Init1;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import javafx.util.Pair;
import org.bouncycastle.math.ec.ECPoint;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.TimeoutException;

public class LocalClientHandlerConnecting extends LocalClientHandler {
    private byte[] randomBytes;
    private byte[] alphaBytes;

    public LocalClientHandlerConnecting(LocalTeamspeakClientSocket client) {
        super(client);
    }

    @Override
    public void onAssigned() throws IOException, TimeoutException {
        PacketBody8Init1 packet = new PacketBody8Init1(ProtocolRole.CLIENT);

        // Initialize connection

        PacketBody8Init1.Step0 step = new PacketBody8Init1.Step0();
        Random random = new Random();
        randomBytes = new byte[4];
        random.nextBytes(randomBytes);
        step.setRandom(randomBytes);
        step.setTimestamp((int) ((System.currentTimeMillis() / 1000L) & 0x000000FFFFFF));
        packet.setStep(step);

        sendInit1(packet);
    }

    private void sendInit1(PacketBody8Init1 packet) throws IOException, TimeoutException {
        packet.setVersion(new byte[] { 0x09, (byte)0x83, (byte)0x8C, (byte)0xCF });

        getClient().writePacket(packet);
    }

    @Override
    public void handlePacket(Packet packet) throws IOException, TimeoutException {
        if (packet.getBody() instanceof PacketBody8Init1) {
            PacketBody8Init1 init1 = (PacketBody8Init1) packet.getBody();

            switch (init1.getStep().getNumber()) {
                case 1:
                    PacketBody8Init1.Step1 serverReplyStep1 = (PacketBody8Init1.Step1)init1.getStep();
                    // Check nonce.  It's received backwards, so walk backwards over the array received
                    for (int i = 0; i < 4; i ++) {
                        if (randomBytes[3 - i] != serverReplyStep1.getA0reversed()[i])
                            throw new IllegalArgumentException("invalid server nonce");
                    }

                    // Build response
                    PacketBody8Init1 response2 = new PacketBody8Init1(ProtocolRole.CLIENT);

                    PacketBody8Init1.Step2 step2 = new PacketBody8Init1.Step2();
                    step2.setA0reversed(serverReplyStep1.getA0reversed());
                    step2.setServerStuff(serverReplyStep1.getServerStuff());
                    response2.setStep(step2);

                    sendInit1(response2);
                    break;
                case 3:
                    PacketBody8Init1.Step3 serverReplyStep3 = (PacketBody8Init1.Step3)init1.getStep();

                    // Calculate 'y'
                    // which is the result of x ^ (2 ^ level) % n as an unsigned
                    // BigInteger. Padded from the lower side with '0x00' when shorter
                    // than 64 bytes.
                    // CITE: https://github.com/Splamy/TS3AudioBot/blob/master/TS3Client/Full/Ts3Crypt.cs
                    // Prepare solution
                    if (serverReplyStep3.getLevel() < 0 || serverReplyStep3.getLevel() > 1_000_000)
                        throw new IllegalArgumentException("RSA challenge level is not within an acceptable range");

                    BigInteger x = new BigInteger(1, serverReplyStep3.getX());
                    BigInteger n = new BigInteger(1, serverReplyStep3.getN());

                    byte[] y = new byte[64];

                    byte[] solution =
                            x.modPow(BigInteger.valueOf(2L).pow(serverReplyStep3.getLevel()), n).toByteArray();

                    System.arraycopy(
                            solution, Math.abs(solution.length - 64),
                            y, solution.length < 64 ? 64 - solution.length : 0,
                            solution.length < 64 ? solution.length : 64
                    );

                    // Build response
                    PacketBody8Init1 response4 = new PacketBody8Init1(ProtocolRole.CLIENT);

                    PacketBody8Init1.Step4 step4 = new PacketBody8Init1.Step4();

                    step4.setLevel(serverReplyStep3.getLevel());
                    step4.setX(serverReplyStep3.getX());
                    step4.setN(serverReplyStep3.getN());
                    step4.setY(y);
                    step4.setServerStuff(serverReplyStep3.getServerStuff());

                    alphaBytes = new byte[10];
                    new Random().nextBytes(alphaBytes);

                    SingleCommand initiv = new SingleCommand("clientinitiv", ProtocolRole.CLIENT);

                    initiv.add(new CommandSingleParameter("alpha", Base64.getEncoder().encodeToString(alphaBytes)));

                    initiv.add(new CommandSingleParameter("omega", getClient().getIdentity().getPublicKeyString()));

                    initiv.add(new CommandSingleParameter("ot", "1")); // constant, set to 1

                    initiv.add(
                            new CommandSingleParameter(
                                "ip",
                                getClient().getOption("client.hostname", String.class)
                            )
                    );

                    step4.setClientIVcommand(initiv.build().getBytes(Charset.forName("UTF8")));

                    response4.setStep(step4);

                    sendInit1(response4);
                    break;
                default:
                    throw new IllegalArgumentException("unexpected Init1 server step: " + init1.getStep().getNumber());

            }
        } else if (packet.getBody() instanceof PacketBody2Command) {
            SingleCommand command = ((PacketBody2Command) packet.getBody()).parse().simplifyOne();

            if (command.getName().equalsIgnoreCase("initivexpand2")) {
                // 3.2.2 initivexpand2 (Client <- Server)

                if (!command.get("ot").getValue().equals("1"))
                    throw new IllegalArgumentException("ot constant != 1: " + command.get("ot").getValue());

                byte[] license = Base64.getDecoder().decode(command.get("l").getValue());
                /*byte[] licsense_validation = !command.has("tvd") && command.get("tvd").getValue() != null ?
                        null :
                        Base64.getDecoder().decode(command.get("tvd").getValue());*/

                byte[] beta = Base64.getDecoder().decode(command.get("beta").getValue()); // beta
                byte[] omega = Base64.getDecoder().decode(command.get("omega").getValue()); // omega
                byte[] proof = Base64.getDecoder().decode(command.get("proof").getValue());

                Pair<byte[], byte[]> keyPair = Ts3Crypt.generateKeypair();

                // 3.2.1.1 Verify integrity
                // The proof parameter is the sign of the l parameter (not base64 encoded). The client can verify the l
                // parameter with the public key of the server which is sent in omega.

                ECPoint publicKey = Ts3Crypt.decodePublicKey(omega);

                if (!Ts3Crypt.verifySignature(publicKey, license, proof))
                    throw new SecurityException("invalid proof signature: " + Ts3Debugging.getHex(proof));

                byte[] signature = Ts3Crypt.generateClientEkProof(
                        keyPair.getKey(),
                        beta,
                        getClient().getIdentity()
                );

                getClient().sendCommand(
                        new SingleCommand(
                                "clientek", ProtocolRole.CLIENT,
                                new CommandSingleParameter("ek", Base64.getEncoder().encodeToString(keyPair.getKey())),
                                new CommandSingleParameter("proof", Base64.getEncoder().encodeToString(signature))
                        )
                );

                getClient().setSecureParameters(Ts3Crypt.cryptoInit2(license, alphaBytes, beta, keyPair.getValue()));

                getClient().sendCommand(new SingleCommand(
                                "clientinit",
                                ProtocolRole.CLIENT,
                                new CommandSingleParameter("client_nickname", getClient().getNickname()),
                                new CommandSingleParameter("client_version", "3.1.8 [Build: 1516614607]"),
                                new CommandSingleParameter("client_platform", "Windows"),
                                new CommandSingleParameter(
                                        "client_version_sign",
                                                "LJ5q+KWT4KwBX7oR/9j9A12hBrq5ds5ony99" +
                                                "f9kepNmqFskhT7gfB51bAJNgAMOzXVCeaItNmc10F2wUNktqCw=="
                                ),
                                new CommandSingleParameter("client_input_hardware", "1"),
                                new CommandSingleParameter("client_output_hardware", "1"),
                                new CommandSingleParameter("client_default_channel",
                                        getClient().getOption("client.default_channel", String.class)
                                ),
                                new CommandSingleParameter("client_default_channel_password",
                                        getClient().getOption("client.default_channel_password", String.class)
                                ),
                                new CommandSingleParameter(
                                        "client_server_password",
                                        getClient().getOption("client.server_password", String.class)
                                ),
                                new CommandSingleParameter("client_nickname_phonetic",
                                        getClient().getOption("client.nickname_phonetic", String.class)
                                ),
                                new CommandSingleParameter("client_meta_data", ""),
                                new CommandSingleParameter("client_default_token",
                                        getClient().getOption("client.default_token", String.class)
                                ),
                                new CommandSingleParameter(
                                        "client_key_offset",
                                        Long.toString(getClient().getIdentity().getKeyOffset())
                                ),
                                new CommandSingleParameter(
                                        "hwid",
                                        getClient().getOption("client.hwid", String.class) != null ?
                                                getClient().getOption("client.hwid", String.class) :
                                                "+LyYqbDqOvEEpN5pdAbF8/v5kZ0="
                                )
                        )
                );

                getClient().setCommandProcessor(CommandHandler.createLocalClientHandler());
                getClient().setState(ClientConnectionState.RETRIEVING_DATA);
            } else if (command.getName().equals("error")) {
                getClient().setState(ClientConnectionState.DISCONNECTED);

                throw new IOException(command.get("msg").getValue());
            }
        }
    }
}
