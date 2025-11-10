package space.yahho.mcmod.novaio.number;

import static space.yahho.mcmod.novaio.NovaIO.LOGGER;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class NovaIoNumberManager {
    private final HashMap<UUID, HeavyBigInteger> loadedNumbers = new HashMap<>();
    private final Path numStorePath;

    public NovaIoNumberManager(Path storePath) {
        this.numStorePath = storePath;
        loadAllStoredNumbers(listStoredDatafiles());
    }

    private List<File> listStoredDatafiles() {
        File[] files = this.numStorePath.toFile().listFiles();
        if (files == null) return new ArrayList<>();
        return Arrays.stream(files)
                .filter((file) ->{
                    if (file.isDirectory()) return false;
                    if (file.isFile() && file.getName().endsWith(".dat.zstd")) {
                        try {
                            UUID.fromString(file.getName().substring(0, 36).toUpperCase());
                            return true;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                    return false;
                })
                .toList();
    }

    private void loadStoredNumber(File file, boolean overwrite) throws IOException, ClassNotFoundException {
        UUID uuid = UUID.fromString(file.getName().substring(0, 36).toUpperCase());
        if (this.loadedNumbers.containsKey(uuid) && !overwrite) return;
        ObjectInputStream inputStream = new ObjectInputStream(new ZstdInputStream(Files.newInputStream(file.toPath(), StandardOpenOption.READ)));
        this.loadedNumbers.put(uuid, (HeavyBigInteger) inputStream.readObject());
        inputStream.close();
    }

    private void loadAllStoredNumbers(List<File> files) {
        files.forEach(file -> {
            try {
                loadStoredNumber(file, true);
            } catch (IOException | ClassNotFoundException e) {
                LOGGER.error(e.getMessage());
            }
        });
    }

    public void saveStoredNumber(UUID uuid) throws IOException {
        Path datafile = numStorePath.resolve(uuid.toString() + ".dat.zstd");
        LOGGER.debug("Saving data to: {}", datafile);
        if (Files.exists(datafile)) {
            ObjectOutputStream oos = new ObjectOutputStream(new ZstdOutputStream(Files.newOutputStream(datafile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)));
            oos.writeObject(this.loadedNumbers.get(uuid));
            oos.flush();
            oos.close();
        } else {
            ObjectOutputStream oos = new ObjectOutputStream(new ZstdOutputStream(Files.newOutputStream(datafile, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)));
            oos.writeObject(this.loadedNumbers.get(uuid));
            oos.flush();
            oos.close();
        }
    }

    public void saveAllStoredNumbers() {
        LOGGER.info("Saving all stored numbers...");
        this.loadedNumbers.keySet().forEach(uuid -> {
            try {
                saveStoredNumber(uuid);
            } catch (IOException e) {
                LOGGER.error("I/O error has occurred during saving data. id: '{}', error: {}", uuid, e.getMessage());
            }
        });
        LOGGER.info("All on-memory numbers saved!");
    }

    public boolean claimAndStoreNumber(UUID uuid, HeavyBigInteger heavyBigInteger) {
        if (this.loadedNumbers.containsKey(uuid)) return false;
        else {
            this.loadedNumbers.put(uuid, heavyBigInteger);
            return true;
        }
    }

    public Optional<HeavyBigInteger> getStoredNumber(UUID uuid) {
        if (this.loadedNumbers.containsKey(uuid)) return Optional.ofNullable(this.loadedNumbers.get(uuid));
        else return Optional.empty();
    }

    public boolean updateStoredNumber(UUID uuid, HeavyBigInteger heavyBigInteger) {
        if (!this.loadedNumbers.containsKey(uuid)) return false;
        else {
            this.loadedNumbers.put(uuid, heavyBigInteger);
            return true;
        }
    }

    public boolean updateStoredNumber(UUID uuid, Path source) {
        if (!this.loadedNumbers.containsKey(uuid)) return false;
        else {
            try {
                long len = Files.size(source);
                try (SeekableByteChannel ch = Files.newByteChannel(source, StandardOpenOption.READ)) {
                    ByteBuffer bb = ByteBuffer.allocate((int) Math.min(256_000_000, len));
                    HeavyBigInteger heavyBigInteger = null;
                    LOGGER.info("Loading data(size: {} Bytes) from: {}", len, source);

                    do {
                        bb.clear();
                        ch.read(bb);
                        bb.flip();
                        byte[] bytes = new byte[bb.remaining()];
                        bb.get(bytes);
                        LOGGER.debug("Trying to load {} bytes...", bytes.length);
                        if (heavyBigInteger == null) heavyBigInteger = new HeavyBigInteger(new BigInteger(1, bytes));
                        else {
                            int retries = 0;
                            while (true) {
                                try {
                                    heavyBigInteger = heavyBigInteger.shiftLeft(BigInteger.valueOf(bytes.length * 8L)).add(new HeavyBigInteger(new BigInteger(1, bytes)));
                                    break;
                                } catch (ArithmeticException e) {
                                    if (retries >= 3) {
                                        LOGGER.error("Unable to load number part! Calculation failed repeatedly!");
                                        throw new Exception("Calculation failed repeatedly!");
                                    }
                                    LOGGER.debug("Something went wrong! Retrying...");
                                    retries++;
                                }
                            }
                        }
                        len -= bytes.length;
                        LOGGER.debug("{} bytes loaded, {} bytes remaining...", bytes.length, len);
                    } while (len > 0);

                    updateStoredNumber(uuid, heavyBigInteger);
                    saveStoredNumber(uuid);
                    return true;
                }
            } catch (IOException e) {
                LOGGER.error("I/O error has occurred during assigning file data to number. ID: {}, error: {}", uuid, e.getMessage());
                return false;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public void setDisappeared(UUID uuid) {
        this.loadedNumbers.remove(uuid);
        this.deleteUnneededDatafile(uuid);
    }

    public void deleteUnneededDatafile(UUID uuid) {
        Path datafile = numStorePath.resolve(uuid.toString() + ".dat.zstd");
        if (Files.exists(datafile)) {
            try {
                Files.delete(datafile);
            } catch (IOException e) {
                LOGGER.error("I/O error has occurred during deleting data. ID: \"{}\", error: {}", uuid, e.getMessage());
            }
        }
    }

    public Set<UUID> getLoadedNumbers() {
        return this.loadedNumbers.keySet();
    }
}
