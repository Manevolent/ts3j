package com.github.manevolent.ts3j.command.type.server;

import com.github.manevolent.ts3j.command.TypedCommand;
import com.github.manevolent.ts3j.command.part.CommandSingleParameter;
import com.github.manevolent.ts3j.protocol.ProtocolRole;

public class ServerInitIVExpand extends TypedCommand {
    public ServerInitIVExpand() {
        super(ProtocolRole.CLIENT, "initivexpand");

        set(new CommandSingleParameter("alpha"));
        set(new CommandSingleParameter("beta"));
        set(new CommandSingleParameter("omega"));
    }
}
