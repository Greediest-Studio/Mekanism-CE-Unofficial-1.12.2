package mekanism.common.security;

import io.netty.buffer.ByteBuf;
import mekanism.common.security.ISecurityTile.SecurityMode;
import mekanism.common.util.MekanismUtils;

public class SecurityData {

    public SecurityMode mode = SecurityMode.PUBLIC;
    public boolean override;

    public SecurityData() {
    }

    public SecurityData(SecurityFrequency frequency) {
        mode = frequency.securityMode;
        override = frequency.override;
    }

    public static SecurityData read(ByteBuf dataStream) {
        SecurityData data = new SecurityData();
        data.mode = MekanismUtils.getByIndex(SecurityMode.values(), dataStream.readInt(), data.mode);
        data.override = dataStream.readBoolean();
        return data;
    }

    public void write(ByteBuf dataStream) {
        dataStream.writeInt(mode.ordinal());
        dataStream.writeBoolean(override);
    }
}
