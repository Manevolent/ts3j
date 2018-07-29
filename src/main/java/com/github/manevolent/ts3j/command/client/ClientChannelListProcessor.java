package com.github.manevolent.ts3j.command.client;

import com.github.manevolent.ts3j.command.CommandProcessException;
import com.github.manevolent.ts3j.command.CommandProcessor;
import com.github.manevolent.ts3j.command.SingleCommand;
import com.github.manevolent.ts3j.enums.CodecType;
import com.github.manevolent.ts3j.model.Channel;
import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;

public class ClientChannelListProcessor implements CommandProcessor {
    @Override
    public void process(AbstractTeamspeakClientSocket client,
                        SingleCommand command) throws CommandProcessException {
        int cid = Integer.parseInt(command.get("cid").getValue());

        Channel channel = client.getVirtualServer().getChannelById(cid);
        if (channel == null)
            channel = new Channel(
                    client.getVirtualServer(), Integer.parseInt(command.get("cid").getValue())
            );

        channel.setName(command.get("channel_name").getValue());
        channel.setCodec(CodecType.fromId(Integer.parseInt(command.get("channel_codec").getValue())));
        channel.setCodecQuality(Integer.parseInt(command.get("channel_codec_quality").getValue()));
        channel.setMaxClients(Integer.parseInt(command.get("channel_maxclients").getValue()));
        channel.setMaxFamilyClients(Integer.parseInt(command.get("channel_maxfamilyclients").getValue()));
        channel.setOrder(Integer.parseInt(command.get("channel_order").getValue()));
        channel.setPermanent(command.get("channel_flag_permanent").getValue().equals("1"));
        channel.setSemiPermanent(command.get("channel_flag_semi_permanent").getValue().equals("1"));
        channel.setServerDefault(command.get("channel_flag_default").getValue().equals("1"));
        channel.setHasPassword(command.get("channel_flag_password").getValue().equals("1"));
        channel.setCodecLatencyFactor(command.get("channel_codec_latency_factor").getValue().equals("1"));
        channel.setUnencrypted(command.get("channel_codec_is_unencrypted").getValue().equals("1"));
        channel.setDeleteDelay(Integer.parseInt(command.get("channel_delete_delay").getValue()));
        channel.setUnlimited(command.get("channel_flag_maxclients_unlimited").getValue().equals("1"));
        channel.setFamilyUnlimited(command.get("channel_flag_maxfamilyclients_unlimited").getValue().equals("1"));
        channel.setInheritingMaxfamilyUnlimited(command.get("channel_flag_maxfamilyclients_inherited").getValue().equals("1"));
        channel.setNeededTalkPower(Integer.parseInt(command.get("channel_needed_talk_power").getValue()));
        channel.setForcingSilence(command.get("channel_forced_silence").getValue().equals("1"));
        channel.setIconId(Integer.parseInt(command.get("channel_icon_id").getValue()));
        channel.setPrivated(command.get("channel_flag_private").getValue().equals("1"));

        client.getVirtualServer().updateChannel(channel);
    }
}
