package com.github.manevolent.ts3j.protocol.packet.handler.local;

import com.github.manevolent.ts3j.command.SimpleCommand;
import com.github.manevolent.ts3j.command.part.CommandSingleParameter;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.packet.PacketBody2Command;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class LocalClientHandlerNegotiating extends LocalClientHandler {
    public LocalClientHandlerNegotiating(LocalTeamspeakClientSocket client) {
        super(client);
    }

    @Override
    public void onAssigned() throws IOException, TimeoutException {
        PacketBody2Command clientinit = new PacketBody2Command(ProtocolRole.CLIENT);

        clientinit.setText(
                new SimpleCommand(
                        "clientinit", ProtocolRole.CLIENT,
                        new CommandSingleParameter("client_nickname", "test"),
                        new CommandSingleParameter("client_version", "3.0.19.3"),
                        new CommandSingleParameter("client_platform", "Windows"),
                        new CommandSingleParameter("client_version_sign", "a1OYzvM18mrmfUQBUgxYBxYz2DUU6y5k3/mEL6FurzU0y97Bd1FL7+PRpcHyPkg4R+kKAFZ1nhyzbgkGphDWDg=="),
                        new CommandSingleParameter("client_input_hardware", "true"),
                        new CommandSingleParameter("client_output_hardware", "false"),
                        new CommandSingleParameter("client_default_channel", ""),
                        new CommandSingleParameter("client_default_channel_password", ""),
                        new CommandSingleParameter("client_server_password", ""),
                        new CommandSingleParameter("client_nickname_phonetic", "test"),
                        new CommandSingleParameter("client_meta_data", ""),
                        new CommandSingleParameter("client_default_token", ""),
                        new CommandSingleParameter("client_key_offset", Long.toString(getClient().getIdentity().getKeyOffset())),
                        new CommandSingleParameter("hwid", "87056c6e1268aaf5055abf8256415e0e,408978b6d98810cc03f0aa16a4c75600")
                ).build()
        );

        getClient().writePacket(clientinit);
    }
}
