package com.github.manevolent.ts3j.command;

import com.github.manevolent.ts3j.command.part.CommandParameter;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.client.TeamspeakClient;
import com.github.manevolent.ts3j.protocol.server.TeamspeakServer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class TypedCommand implements Command {
    private final ProtocolRole role;
    private final String name;
    private final Map<String, CommandParameter> parameters = new LinkedHashMap<>();

    protected TypedCommand(ProtocolRole role, String name) {
        this.role = role;

        this.name = name;
    }

    protected CommandParameter set(CommandParameter commandParameter) {
        this.parameters.put(commandParameter.getName(), commandParameter);

        return commandParameter;
    }

    public CommandParameter get(String key) {
        return this.parameters.get(key);
    }

    @Override
    public Iterable<CommandParameter> getParameters() {
        return Collections.unmodifiableCollection(parameters.values());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ProtocolRole getRole() {
        return role;
    }

    @Override
    public String toString() {
        return build();
    }

    public void handleServer(TeamspeakServer server) {

    }

    public void handleClient(TeamspeakClient client) {

    }
}
