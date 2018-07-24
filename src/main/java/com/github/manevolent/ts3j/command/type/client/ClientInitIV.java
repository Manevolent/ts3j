package com.github.manevolent.ts3j.command.type.client;

import com.github.manevolent.ts3j.command.TypedCommand;
import com.github.manevolent.ts3j.command.part.CommandSingleParameter;
import com.github.manevolent.ts3j.protocol.ProtocolRole;

public class ClientInitIV extends TypedCommand {
    public ClientInitIV() {
        super(ProtocolRole.CLIENT, "clientinitiv");

        set(new CommandSingleParameter("ot"));
        set(new CommandSingleParameter("alpha"));
        set(new CommandSingleParameter("omega"));
        set(new CommandSingleParameter("ip"));
    }
}
