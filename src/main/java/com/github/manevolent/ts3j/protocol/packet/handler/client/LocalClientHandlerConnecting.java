package com.github.manevolent.ts3j.protocol.packet.handler.client;

import com.github.manevolent.ts3j.command.type.client.ClientInitIV;
import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.client.LocalTeamspeakClient;
import com.github.manevolent.ts3j.protocol.packet.Packet8Init1;
import com.github.manevolent.ts3j.util.Ts3Logging;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Base64;
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
        Ts3Logging.debug("Connecting: handling " + packet.getClass().getSimpleName());

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

                    ClientInitIV command = new ClientInitIV();

                    command.get("alpha").set(Base64.getEncoder().encodeToString(alphaBytes));
                    command.get("omega").set(getClient().getLocalIdentity().getPublicKeyString());
                    command.get("ot").set(1); // ?
                    command.get("ip").set(null);

                    step4.setClientIVcommand(command.build().getBytes(Charset.forName("UTF8")));

                    response4.setStep(step4);

                    sendInit1(response4);
                    break;
                default:
                    throw new IllegalArgumentException("unexpected Init1 server step: " + init1.getStep().getNumber());

            }
        }
    }
}
