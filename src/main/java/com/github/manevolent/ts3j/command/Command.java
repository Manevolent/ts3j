package com.github.manevolent.ts3j.command;

import com.github.manevolent.ts3j.command.parameter.CommandParameter;
import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.util.LinkedHashMap;
import java.util.Map;

public interface Command {
    /**
     * Gets the name, or label, of this command.
     * @return Command label.
     */
    String getName();

    /**
     * Gets the intended use role of this command (source).
     * @return Command role.
     */
    ProtocolRole getRole();

    /**
     * Gets the parameter list of this command.
     * @return Parameter list.
     */
    Iterable<CommandParameter> getParameters();

    default Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();

        for (CommandParameter parameter : getParameters())
            map.put(parameter.getName(), parameter.getValue());

        return map;
    }

    void add(CommandParameter parameter);

    /**
     * Builds the command to a netowrk-issueable string.
     * The implementation currently defaults to the basic command format for TS3:
     * label -flag parameter=value
     * @return Baked command string.
     */
    default String build() {
        StringBuilder builder = new StringBuilder();
        builder.append(getName());

        for (CommandParameter parameter : getParameters()) {
            builder.append(" " + (parameter.toString()).trim());
        }

        return builder.toString();
    }
}
