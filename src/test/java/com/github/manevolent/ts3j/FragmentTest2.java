package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.packet.fragment.PacketBodyFragment;
import com.github.manevolent.ts3j.protocol.packet.fragment.PacketReassembly;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;
import org.bouncycastle.util.encoders.Base64;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FragmentTest2 extends TestCase {

    public static void main(String[] args) throws Exception {
        new FragmentTest2().testParser();
    }

    public void testParser() throws Exception {
        Ts3Debugging.setEnabled(true);

        String[] packets = new String[] {
                "LvdqqRB1ozMABBJlaXY0e5iIcGFja2V0c4FQkRkjgz0zMnE7IFGYbmVjIviVXFI5Ag8P3F8kgzQ4Mzg5OSFgkIgSRjSFKjI0MjgxYJiIVOJNNDE2NTA0OXFgmYh7gl+iZYsxz/mBUFGYZIUqMTkzUlOQiB5taW4hIocqMTgBYJCIFEc0oGUSMjkRYJCIIrv4MjmhYJiIUZiCNQMHAIAxGST6MDI5MTI2QWCZiKPzX2lwPTc0LjIwOS4yLjg1",
                "xX0QvWydOFsAAlJHhAQAAEoHAAAAAACgY2lkPTEgY2xpZW50X2lkbGVfdGltZT0xNjgyMjdmXnVgAACAbmlxdWWR8TGSaWZpZXI9SW9RY3NZMnYwcXpNMThOeGFsAAQCgFBQazBiTEpLQT1mXm5pY2tuYbFrbm90XHNhXHN3aG9yZQEAAIBmXnZlcnNpb249My4xLjEwXHNbQnVpbGQ6XHMxNTI4NUAAgIAzNzYxNV1mXnBsYXRmb3JtPVdpbmRvd3NmXmlucHV0X22IhgOWdXRleedvdXR4IzBmXoQib25seZWgJ2D0lmhhcmR3YXJh52xeX+BkEmRlAAACiGZhdWx0X2NoYW5uZWw9XC84Zl5tZXRhX2RhdGFmXmlzXwAMAIByZWNvcmRpbmc9J2AFJV9zaWduPStcL0JXdnVlaWtHZzQAAACAWWtPMXYydXVaQjV2dEpKZ1VaNWJMOGNSZnhBc3RmbgAAAIBDVmRybzJqYSs0YSs4ckdVekR4OFwvdnZUWk9VVkQ2AECAhFU5NWhuV2I2MzhNQ1E992BzZWN1cml0eZHkc2hmXmxvZ2kcGUiMbl+Ce2ZeImJiYXMCnz0yZl5V519ncm91cJHxPTVmXnNlcgElA0lzPTYhIlCgZl5jcmVhIzM1MzAhYTc1OGZebGE=",
                "+hN1ybZO1gEAAwJzdGNvgYNjJTM0MDg5MDMnYHQwwoCQb3RhbFWY8ZhzPTV3YGF3YXnoI2IXX21lc3NhZ3dldHlwZegjZmwgAACAYWdfYXZhJ3I9ZmQwMjhjYzUzNmU3YmM3MWJiOWQ4MGGQQZOhZWFjYxEUNTNmXiF7a19wb3chGzd3YCN7cmVhMnN06CMqe19tc2dmXmSAAELgZXNjcmlwdPGYPW1hbmVcc2lzs1RnaWFuEbRuaWdnZXJmXuFpBwDggiJ7IRsnYG1vbnRoX2J5dGVzX3VwbG9hZDF7IGAVIYFupZcxNzI3Mg4PwYE0Zl4jg55cMzc4NldgKoMggRnhaXByaW9D43NwZWFrIRs3YIb1X3Bob25liAYM+nRpY2ZebmVlITJfVCVhMnJ5X3ZpZXePaiYwdG9rZW5mXmlRmJLxJ2DhadEAHORW52NvbbF4ZCEbJ2Bjb3VudHJ5PVVTZl5W5wNJX2luaGVB42Vkll2S8QHCgIA3YGJhZGdlcz1vASV3b2xm6CNCZTY0SGFzaEOj81VJRD1jY2kAAACAZWJtbGJpbmtwbmNrbW1tbmhtZGhiZ2tmZG1wamRlZ+AAAKBtbGNlazFllIjxmF9maWxldHJhbnNmZXJfYmFuZHdpZCHYc48BR/ExkuIjkIghQTQ=",
        };

        List<Packet> pieces = new ArrayList<>();

        for (int i = 0; i < packets.length; i ++) {
            Packet packet = new Packet(ProtocolRole.SERVER);
            ByteBuffer buffer = ByteBuffer.wrap(Base64.decode(packets[i]));
            packet.readHeader(buffer);
            packet.setBody(new PacketBodyFragment(packet.getHeader().getType(), ProtocolRole.SERVER));
            packet.readBody(buffer);
            pieces.add(packet);

            Ts3Debugging.debug(
                    "Read type=" + packet.getHeader().getType()
                    + " id=" + packet.getHeader().getPacketId()
                    + " flags=" + packet.getHeader().getPacketFlags()
                    + " fragment=" + packet.getHeader().getPacketFlag(HeaderFlag.FRAGMENTED)
            );
        }

        Ts3Debugging.debug("Loaded " + pieces.size() + " pieces.");

        PacketReassembly reassembly = new PacketReassembly();
        reassembly.setPacketId(2);

        for (Packet piece : pieces) {
            reassembly.put(piece);
        }

        Packet reassembled;
        while (((reassembled = reassembly.next()) != null)) {
            Ts3Debugging.debug(reassembled.getHeader().getPacketId());
        }
    }

}
