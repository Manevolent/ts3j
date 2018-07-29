package com.github.manevolent.ts3j.command.client;

import com.github.manevolent.ts3j.command.CommandProcessException;
import com.github.manevolent.ts3j.command.CommandProcessor;
import com.github.manevolent.ts3j.command.MultiCommand;
import com.github.manevolent.ts3j.command.SingleCommand;
import com.github.manevolent.ts3j.enums.HostBannerMode;
import com.github.manevolent.ts3j.enums.HostMessageMode;
import com.github.manevolent.ts3j.enums.VoiceEncryptionMode;
import com.github.manevolent.ts3j.model.VirtualServer;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;

public class ClientInitServerProcessor implements CommandProcessor {
    @Override
    public void process(AbstractTeamspeakClientSocket client,
                        MultiCommand multiCommand) throws CommandProcessException {
        SingleCommand command = multiCommand.simplifyOne();

        VirtualServer server = new VirtualServer();

        server.setName(command.get("virtualserver_name").getValue());
        server.setWelcomeMessage(command.get("virtualserver_welcomemessage").getValue());
        server.setPlatform(command.get("virtualserver_platform").getValue());
        server.setVersion(command.get("virtualserver_version").getValue());
        server.setMaxClients(Integer.parseInt(command.get("virtualserver_maxclients").getValue()));
        server.setCreated(Long.parseLong(command.get("virtualserver_created").getValue()));
        server.setEncryptionMode(VoiceEncryptionMode.fromId(Integer.parseInt(command.get("virtualserver_codec_encryption_mode").getValue())));
        server.setHostMessageMode(HostMessageMode.fromId(Integer.parseInt(command.get("virtualserver_hostmessage_mode").getValue())));
        server.setDefaultGroupId(Integer.parseInt(command.get("virtualserver_default_server_group").getValue()));
        server.setDefaultChannelGroupId(Integer.parseInt(command.get("virtualserver_default_channel_group").getValue()));
        server.setHostbannerUrl(command.get("virtualserver_hostbanner_gfx_url").getValue());
        server.setPrioritySpeakerDimmModifier(Double.parseDouble(command.get("virtualserver_priority_speaker_dimm_modificator").getValue()));
        server.setId(Integer.parseInt(command.get("virtualserver_id").getValue()));
        server.setNamePhonetic(command.get("virtualserver_name_phonetic").getValue());
        server.setIconId(Long.parseLong(command.get("virtualserver_icon_id").getValue()));
        server.setServerIp(command.get("virtualserver_ip").getValue());
        server.setAskForPriviledgeKey(Integer.parseInt(command.get("virtualserver_ask_for_privilegekey").getValue()) == 1);
        server.setHostBannerMode(HostBannerMode.fromId(Integer.parseInt(command.get("virtualserver_hostbanner_mode").getValue())));
        server.setDefaultTempChannelDeleteDelay(Integer.parseInt(command.get("virtualserver_channel_temp_delete_delay_default").getValue()));
        server.setNickname(command.get("virtualserver_nickname").getValue());

        client.setVirtualServer(server);

        client.setClientId(Integer.parseInt(command.get("aclid").getValue()));
    }
}
