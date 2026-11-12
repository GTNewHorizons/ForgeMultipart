package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TickSchedulerClockRegressionTest {

    @Test
    void savedClockSurvivesReload(@TempDir Path directory) {
        File file = directory.resolve("multipart.dat").toFile();
        TickScheduler.WorldTickScheduler writer = scheduler(file);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("schedTime", 1234);
        writer.loadTag(tag);
        writer.save();
        TickScheduler.WorldTickScheduler reader = scheduler(file);
        reader.load();
        assertEquals(1234, reader.saveTag().getLong("schedTime"));
    }

    @Test
    void missingOrUnreadableClockStillUsesTheFallback(@TempDir Path directory) throws Exception {
        File file = directory.resolve("multipart.dat").toFile();
        TickScheduler.WorldTickScheduler scheduler = scheduler(file);
        scheduler.load();
        assertEquals(900, scheduler.saveTag().getLong("schedTime"));
        Files.write(file.toPath(), new byte[] { 1, 2, 3 });
        scheduler.load();
        assertEquals(900, scheduler.saveTag().getLong("schedTime"));
    }

    private static TickScheduler.WorldTickScheduler scheduler(File file) {
        return new TickScheduler.WorldTickScheduler(null) {

            @Override
            File saveFile() {
                return file;
            }

            @Override
            void loadTag(NBTTagCompound tag) {
                // Supply a deterministic fallback clock without starting a Minecraft world.
                if (!tag.hasKey("schedTime")) {
                    tag.setLong("schedTime", 900);
                }
                super.loadTag(tag);
            }
        };
    }
}
