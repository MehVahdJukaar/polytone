package net.mehvahdjukaar.polytone.common;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;


public class FilesUtil {


    //helpers

    public static void writeAtomically(Path target, IOConsumer<OutputStream> writeLogic) throws IOException {
        Path dir = target.getParent();
        if (dir == null) {
            dir = Paths.get(System.getProperty("java.io.tmpdir"));
        } else {
            Files.createDirectories(dir);
        }

        Path temp = Files.createTempFile(dir, target.getFileName().toString(), ".tmp");

        try {
            // Write data
            try (OutputStream out = Files.newOutputStream(temp, StandardOpenOption.WRITE)) {
                writeLogic.accept(out);
                out.flush();
            }

            // fsync file contents
            try (FileChannel fc = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                fc.force(true);
            }

            // Atomic replace
            Files.move(temp, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            // fsync directory (best effort)
            try (FileChannel dirFc = FileChannel.open(dir, StandardOpenOption.READ)) {
                dirFc.force(true);
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    public static void writeTextAtomically(Path target,
                                           IOConsumer<Writer> writeLogic) throws IOException {
        writeAtomically(target, out -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                writeLogic.accept(writer);
                writer.flush();
            }
        });
    }



    @FunctionalInterface
    public interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }
}
