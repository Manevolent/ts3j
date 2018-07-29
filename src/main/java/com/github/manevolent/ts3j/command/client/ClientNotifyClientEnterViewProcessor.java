package com.github.manevolent.ts3j.command.client;

import com.github.manevolent.ts3j.command.CommandProcessException;
import com.github.manevolent.ts3j.command.CommandProcessor;
import com.github.manevolent.ts3j.command.SingleCommand;
import com.github.manevolent.ts3j.enums.ClientType;
import com.github.manevolent.ts3j.identity.Uid;
import com.github.manevolent.ts3j.model.Client;
import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ClientNotifyClientEnterViewProcessor implements CommandProcessor {
    @Override
    public void process(AbstractTeamspeakClientSocket client,
                        SingleCommand command) throws CommandProcessException {
        int clientId = Integer.parseInt(command.get("clid").getValue());

        Client newClient = client.getVirtualServer().getClientById(clientId);
        if (newClient == null)
            newClient = new Client(
                    Integer.parseInt(command.get("clid").getValue()),
                    Integer.parseInt(command.get("client_database_id").getValue()),
                    client.getVirtualServer(),
                    new Uid(command.get("client_unique_identifier").getValue()),
                    ClientType.fromId(Integer.parseInt(command.get("client_type").getValue())));

        newClient.setNickname(command.get("client_nickname").getValue());

        newClient.setInputMuted(command.get("client_input_muted").getValue().equals("1"));
        newClient.setOutputMuted(command.get("client_output_muted").getValue().equals("1"));

        newClient.setInputHardware(command.get("client_input_hardware").getValue().equals("1"));
        newClient.setOutputHardware(command.get("client_output_hardware").getValue().equals("1"));

        newClient.setRecording(command.get("client_is_recording").getValue().equals("1"));

        newClient.setChannelGroupId(Integer.parseInt(command.get("client_channel_group_id").getValue()));

        newClient.setServerGroupIds(
                Arrays.stream(command.get("client_servergroups").getValue().split(",")).map(
                        Integer::parseInt
                ).collect(Collectors.toList())
        );

        newClient.setAway(command.get("client_away").getValue().equals("1"));

        newClient.setTalkPower(Integer.parseInt(command.get("client_talk_power").getValue()));

        newClient.setTalkRequested(command.get("client_talk_request").getValue().equals("1"));

        newClient.setTalker(command.get("client_is_talker").getValue().equals("1"));

        newClient.setPrioritySpeaker(command.get("client_is_priority_speaker").getValue().equals("1"));

        newClient.setUnreadMessages(Integer.parseInt(command.get("client_unread_messages").getValue()));

        newClient.setNeededServerQueryViewPower(Integer.parseInt(command.get("client_needed_serverquery_view_power").getValue()));

        newClient.setIconId(Integer.parseInt(command.get("client_icon_id").getValue()));

        newClient.setChannelCommander(command.get("client_is_channel_commander").getValue().equals("1"));

        newClient.setCountry(command.get("client_country").getValue());

        newClient.setInheritedChannelGroupChannelId(Integer.parseInt(command.get("client_channel_group_inherited_channel_id").getValue()));

        client.getVirtualServer().updateConnectedClient(newClient);
    }
}
