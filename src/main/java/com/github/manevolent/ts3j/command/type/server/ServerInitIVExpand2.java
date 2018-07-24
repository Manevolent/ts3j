package com.github.manevolent.ts3j.command.type.server;

import com.github.manevolent.ts3j.command.TypedCommand;
import com.github.manevolent.ts3j.command.part.CommandSingleParameter;
import com.github.manevolent.ts3j.protocol.ProtocolRole;

public class ServerInitIVExpand2 extends TypedCommand {
    public ServerInitIVExpand2() {
        super(ProtocolRole.CLIENT, "initivexpand2");

        set(new CommandSingleParameter("l"));
        set(new CommandSingleParameter("beta"));
        set(new CommandSingleParameter("omega"));
        set(new CommandSingleParameter("ot"));
        set(new CommandSingleParameter("proof"));
        set(new CommandSingleParameter("tvd"));
    }
}
