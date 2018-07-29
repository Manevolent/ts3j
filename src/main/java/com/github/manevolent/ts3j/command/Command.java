package com.github.manevolent.ts3j.command;

import com.github.manevolent.ts3j.command.parameter.CommandParameter;
import com.github.manevolent.ts3j.protocol.ProtocolRole;

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

    /**
     * Finds if the command is expected to be responded to with a specific return_code.
     * This is handled by supporting protocol handlers, not by the command code itself.
     * @return true if the command is expected to support a return_code.
     */
    default boolean willExpectResponse() { return false; }

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
