package com.github.manevolent.ts3j.protocol.packet.handler.client;

import com.github.manevolent.ts3j.command.SimpleCommand;
import com.github.manevolent.ts3j.command.part.CommandSingleParameter;
import com.github.manevolent.ts3j.license.License;
import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.client.LocalTeamspeakClient;
import com.github.manevolent.ts3j.protocol.packet.Packet2Command;
import com.github.manevolent.ts3j.protocol.packet.Packet8Init1;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Logging;
import javafx.util.Pair;
import org.bouncycastle.math.ec.ECPoint;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.Random;

public class LocalClientHandlerConnecting extends LocalClientHandler {
    private byte[] randomBytes;
    private byte[] alphaBytes;

    public LocalClientHandlerConnecting(LocalTeamspeakClient client) {
        super(client);
    }

    @Override
    public void onAssigned() throws IOException {
        Packet8Init1 packet = new Packet8Init1(ProtocolRole.CLIENT);

        // Initialize connection

        Packet8Init1.Step0 step = new Packet8Init1.Step0();
        Random random = new Random();
        randomBytes = new byte[4];
        random.nextBytes(randomBytes);
        step.setRandom(randomBytes);
        step.setTimestamp((int) ((System.currentTimeMillis() / 1000L) & 0x000000FFFFFF));
        packet.setStep(step);

        sendInit1(packet);
    }

    private void sendInit1(Packet8Init1 packet) throws IOException {
        Ts3Logging.debug("Connecting: sending " + packet.getClass().getSimpleName()
                + ":"
                + packet.getStep().getClass().getSimpleName() + "..."
        );

        packet.setVersion(new byte[] { 0x09, (byte)0x83, (byte)0x8C, (byte)0xCF });

        getClient().send(packet);
    }

    @Override
    public void handlePacket(NetworkPacket packet) throws IOException {
        Ts3Logging.debug("Connecting: handling " + packet.getPacket().getClass().getSimpleName());

        if (packet.getPacket() instanceof Packet8Init1) {
            Packet8Init1 init1 = (Packet8Init1) packet.getPacket();

            switch (init1.getStep().getNumber()) {
                case 1:
                    Packet8Init1.Step1 serverReplyStep1 = (Packet8Init1.Step1)init1.getStep();
                    // Check nonce.  It's received backwards, so walk backwards over the array received
                    for (int i = 0; i < 4; i ++) {
                        if (randomBytes[3 - i] != serverReplyStep1.getA0reversed()[i])
                            throw new IllegalArgumentException("invalid server nonce");
                    }

                    // Build response
                    Packet8Init1 response2 = new Packet8Init1(ProtocolRole.CLIENT);

                    Packet8Init1.Step2 step2 = new Packet8Init1.Step2();
                    step2.setA0reversed(serverReplyStep1.getA0reversed());
                    step2.setServerStuff(serverReplyStep1.getServerStuff());
                    response2.setStep(step2);

                    sendInit1(response2);
                    break;
                case 3:
                    Packet8Init1.Step3 serverReplyStep3 = (Packet8Init1.Step3)init1.getStep();

                    // Calculate 'y'
                    // which is the result of x ^ (2 ^ level) % n as an unsigned
                    // BigInteger. Padded from the lower side with '0x00' when shorter
                    // than 64 bytes.
                    // CITE: https://github.com/Splamy/TS3AudioBot/blob/master/TS3Client/Full/Ts3Crypt.cs
                    // Prepare solution
                    if (serverReplyStep3.getLevel() < 0 || serverReplyStep3.getLevel() > 1_000_000)
                        throw new IllegalArgumentException("RSA challange level is not within an acceptable range");

                    BigInteger x = new BigInteger(1, serverReplyStep3.getX());
                    BigInteger n = new BigInteger(1, serverReplyStep3.getN());

                    byte[] y = new byte[64];

                    byte[] solution =
                            x.modPow(BigInteger.valueOf(2L).pow(serverReplyStep3.getLevel()), n).toByteArray();

                    Ts3Logging.debug(Ts3Logging.getHex(solution));

                    System.arraycopy(
                            solution, Math.abs(solution.length - 64),
                            y, solution.length < 64 ? 64 - solution.length : 0,
                            solution.length < 64 ? solution.length : 64
                    );

                    // Build response
                    Packet8Init1 response4 = new Packet8Init1(ProtocolRole.CLIENT);

                    Packet8Init1.Step4 step4 = new Packet8Init1.Step4();

                    step4.setLevel(serverReplyStep3.getLevel());
                    step4.setX(serverReplyStep3.getX());
                    step4.setN(serverReplyStep3.getN());
                    step4.setY(y);
                    step4.setServerStuff(serverReplyStep3.getServerStuff());

                    alphaBytes = new byte[10];
                    new Random().nextBytes(alphaBytes);

                    SimpleCommand initiv = new SimpleCommand("clientinitiv", ProtocolRole.CLIENT);

                    initiv.add(new CommandSingleParameter("alpha", Base64.getEncoder().encodeToString(alphaBytes)));

                    initiv.add(new CommandSingleParameter("omega", getClient().getLocalIdentity().getPublicKeyString()));

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
        } else if (packet.getPacket() instanceof Packet2Command) {
            SimpleCommand command = ((Packet2Command) packet.getPacket()).parse();

            if (command.getName().equalsIgnoreCase("initivexpand2")) {
                // 3.2.2 initivexpand2 (Client <- Server)

                Ts3Logging.debug("initivexpand2");

                if (!command.get("ot").getValue().equals("1"))
                    throw new IllegalArgumentException("ot constant != 1: " + command.get("ot").getValue());

                byte[] license = Base64.getDecoder().decode(command.get("l").getValue());
                byte[] licsense_validation = command.get("tvd").getValue() == null ?
                        null :
                        Base64.getDecoder().decode(command.get("tvd").getValue());

                byte[] beta = Base64.getDecoder().decode(command.get("beta").getValue()); // beta
                byte[] omega = Base64.getDecoder().decode(command.get("omega").getValue()); // omega
                byte[] proof = Base64.getDecoder().decode(command.get("proof").getValue()); // ecdh_sign(1)

                Pair<byte[], byte[]> keyPair = Ts3Crypt.generateKeypair();

                // 3.2.1.1 Verify integrity
                // The proof parameter is the sign of the l parameter (not base64 encoded). The client can verify the l
                // parameter with the public key of the server which is sent in omega.

                ECPoint publicKey = Ts3Crypt.decodePublicKey(omega);

                if (!Ts3Crypt.verifySignature(publicKey, license, proof))
                    throw new SecurityException("invalid proof signature: " + Ts3Logging.getHex(proof));

                // generate proof for clientek
                byte[] toSign = new byte[86];
                System.arraycopy(keyPair.getKey(), 0, toSign, 0, 32);
                System.arraycopy(beta, 0, toSign, 32, 54);
                byte[] sign = getClient().getLocalIdentity().sign(toSign);

                Ts3Logging.debug("Created proof (clientek): " + Ts3Logging.getHex(sign));

                // Send clientek (telling them the shared secret)
                Packet2Command clientek = new Packet2Command(ProtocolRole.CLIENT);
                clientek.setText(
                        new SimpleCommand(
                                "clientek", ProtocolRole.CLIENT,
                                new CommandSingleParameter("ek", Base64.getEncoder().encodeToString(keyPair.getKey())),
                                new CommandSingleParameter("proof", Base64.getEncoder().encodeToString(sign))
                        ).build()
                );
                getClient().send(clientek);

                Ts3Crypt.cryptoInit2(license, alphaBytes, omega, proof, beta, keyPair.getValue());

                // send client init...

                // DONE!

            }
        }
    }
}
