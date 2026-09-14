package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TickSchedulerSaveFailureRegressionTest {

    @Test
    void savePropagatesTheOriginalIoException(@TempDir Path directory) {
        TickScheduler.WorldTickScheduler scheduler = new TickScheduler.WorldTickScheduler(null) {

            @Override
            File saveFile() {
                return directory.toFile();
            }
        };
        assertThrows(IOException.class, scheduler::save);
    }
}
