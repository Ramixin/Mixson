package net.ramixin.mixson;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.util.interfaces.MixsonCodec;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@SuppressWarnings("unused")
public interface MixsonCodecs {

    MixsonCodec<JsonElement> JSON_ELEMENT = new MixsonCodec<>() {
        @Override
        public String extensionAndDot() {
            return ".json";
        }

        @Override
        public JsonElement deserialize(Resource r) throws IOException {
            return JsonParser.parseReader(r.openAsReader());
        }

        @Override
        public Resource serialize(Resource r, JsonElement x) {
            return new Resource(r.source(), () -> new ByteArrayInputStream(x.toString().getBytes()), r::metadata);
        }

        @Override
        public ByteArrayOutputStream export(JsonElement resource) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(new GsonBuilder().setPrettyPrinting().create().toJson(resource).getBytes());
            return baos;
        }
    };

    MixsonCodec<BufferedImage> PNG = new MixsonCodec<>() {
        @Override
        public String extensionAndDot() {
            return ".png";
        }

        @Override
        public BufferedImage deserialize(Resource r) throws IOException {
            return ImageIO.read(r.open());
        }

        @Override
        public Resource serialize(Resource r, BufferedImage x) {
            return new Resource(r.source(), () -> new ByteArrayInputStream(bufferedImageToStream(x).toByteArray()), r::metadata);
        }

        @Override
        public ByteArrayOutputStream export(BufferedImage r) throws IOException {
            return bufferedImageToStream(r);
        }
    };

    MixsonCodec<CompoundTag> NBT = new MixsonCodec<>() {
        @Override
        public String extensionAndDot() {
            return ".nbt";
        }

        @Override
        public CompoundTag deserialize(Resource r) throws IOException {
            return NbtIo.readCompressed(r.open(), NbtAccounter.unlimitedHeap());
        }

        @Override
        public Resource serialize(Resource r, CompoundTag x) {
            return new Resource(r.source(), () -> new ByteArrayInputStream(nbtToStream(x).toByteArray()), r::metadata);
        }

        @Override
        public ByteArrayOutputStream export(CompoundTag r) throws IOException {
            return nbtToStream(r);
        }
    };

    private static ByteArrayOutputStream bufferedImageToStream(BufferedImage image) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", stream);
        return stream;
    }

    private static ByteArrayOutputStream nbtToStream(CompoundTag compound) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NbtIo.writeCompressed(compound, baos);
        return baos;
    }
}
