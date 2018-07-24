package com.github.manevolent.ts3j.command;

import com.github.manevolent.ts3j.command.type.client.*;
import com.github.manevolent.ts3j.command.type.server.*;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Commands {
    private static final Map<String, Class<? extends TypedCommand>> commands = new LinkedHashMap<>();

    static {
        // Client
        commands.put("clientinitiv", ClientInitIV.class);

        // Server
        commands.put("initivexpand", ServerInitIVExpand.class);
        commands.put("initivexpand2", ServerInitIVExpand2.class);
    }
}
