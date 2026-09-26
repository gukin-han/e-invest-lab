package dev.gukin.einvestlab.global.id;

import com.github.f4b6a3.uuid.UuidCreator;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class Ids {

    private Ids() {
    }

    public static UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    public static byte[] toBytes(UUID id) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(id.getMostSignificantBits());
        buffer.putLong(id.getLeastSignificantBits());
        return buffer.array();
    }

    public static UUID fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
