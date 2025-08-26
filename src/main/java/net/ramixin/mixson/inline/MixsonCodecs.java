package net.ramixin.mixson.inline;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastBufferedInputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public interface MixsonCodecs {

    MixsonCodec<JsonElement> JSON_ELEMENT = MixsonCodec.create(
            "json",
            r -> JsonParser.parseReader(r.openAsReader()),
            (r, x) -> new Resource(r.source(), () -> new ByteArrayInputStream(x.toString().getBytes()), r::metadata),
            MixsonCodecs::exportJson
    );

    MixsonCodec<BufferedImage> PNG = MixsonCodec.create("png",
            resource -> ImageIO.read(resource.open()),
            (r, elem) -> new Resource(r.source(), () -> new ByteArrayInputStream(bufferedImageToStream(elem).toByteArray()), r::metadata),
            MixsonCodecs::bufferedImageToStream
    );

    // what the fuck, mojang?
    MixsonCodec<CompoundTag> NBT = MixsonCodec.create("nbt",
            resource -> NbtIo.read(
                    new DataInputStream(
                            new FastBufferedInputStream(
                                    new GZIPInputStream(resource.open()))),
                    new NbtAccounter(Long.MAX_VALUE)),
            (r, elem) -> new Resource(r.source(), () -> new ByteArrayInputStream(nbtToStream(elem).toByteArray()), r::metadata),
            MixsonCodecs::nbtToStream
    );

    private static ByteArrayOutputStream bufferedImageToStream(BufferedImage image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stream;
    }

    static ByteArrayOutputStream exportJson(JsonElement json) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(json.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos;
    }

    private static ByteArrayOutputStream nbtToStream(CompoundTag compound) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NbtIo.writeCompressed(compound, baos);
        return baos;
    }

}
