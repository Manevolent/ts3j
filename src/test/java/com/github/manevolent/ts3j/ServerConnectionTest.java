package com.github.manevolent.ts3j;
;
import com.github.manevolent.ts3j.event.*;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Base64;

import static org.junit.Assert.assertEquals;

public class ServerConnectionTest  {
    public static void main(String[] args) throws Exception {
        try {
            Ts3Debugging.setEnabled(true);

            LocalIdentity identity = LocalIdentity.load(
                    new BigInteger(Base64.getDecoder().decode("Tj6YXM3qyRv8n25L2pH+OEJnRUl4auQf8+znjYrOmWU="))
            );

            identity.improveSecurity(10);

            LocalTeamspeakClientSocket client = new LocalTeamspeakClientSocket();

            client.setIdentity(identity);

            client.setNickname("Hello from Java");

            client.setOption("client.hwid", "JAVAJAVAJAVA");

            client.connect(
                    new InetSocketAddress(
                            "ts.teamlixo.net",
                            9987
                    ),
                    null,
                    10000L
            );

            client.addListener(new TS3Listener() {
                @Override
                public void onTextMessage(TextMessageEvent e) {

                }

                @Override
                public void onClientJoin(ClientJoinEvent e) {

                }

                @Override
                public void onClientLeave(ClientLeaveEvent e) {

                }

                @Override
                public void onServerEdit(ServerEditedEvent e) {

                }

                @Override
                public void onChannelEdit(ChannelEditedEvent e) {

                }

                @Override
                public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {

                }

                @Override
                public void onClientMoved(ClientMovedEvent e) {

                }

                @Override
                public void onChannelCreate(ChannelCreateEvent e) {

                }

                @Override
                public void onChannelDeleted(ChannelDeletedEvent e) {

                }

                @Override
                public void onChannelMoved(ChannelMovedEvent e) {

                }

                @Override
                public void onChannelPasswordChanged(ChannelPasswordChangedEvent e) {

                }

                @Override
                public void onChannelList(ChannelListEvent e) {

                }

                @Override
                public void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent e) {

                }

                @Override
                public void onChannelGroupList(ChannelGroupListEvent e) {

                }

                @Override
                public void onServerGroupList(ServerGroupListEvent e) {

                }

                @Override
                public void onClientNeededPermissions(ClientNeededPermissionsEvent e) {

                }

                @Override
                public void onClientChannelGroupChanged(ClientChannelGroupChangedEvent e) {

                }

                @Override
                public void onClientChanged(ClientUpdatedEvent e) {

                }

                @Override
                public void onDisconnected(DisconnectedEvent e) {

                }

                @Override
                public void onChannelSubscribed(ChannelSubscribedEvent e) {

                }
            });

            assertEquals(client.getState(), ClientConnectionState.CONNECTED);

            client.subscribeAll();

            for (int i = 0; i < 1000; i ++) client.getClientInfo(client.getClientId());

            Thread.sleep(100000000L);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
