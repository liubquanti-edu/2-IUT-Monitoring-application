package fr.iut.netmonitor.persistence;

import fr.iut.netmonitor.exception.NetMonitorException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class SerializedDataStore implements DataStore {

    private final Path file;
    private final Path legacyDevicesFile;

    public SerializedDataStore(Path file) {
        this(file, null);
    }

    /** @param legacyDevicesFile ancien fichier ne contenant que les équipements, importé s'il existe */
    public SerializedDataStore(Path file, Path legacyDevicesFile) {
        this.file = file;
        this.legacyDevicesFile = legacyDevicesFile;
    }

    @Override
    public AppData load() throws NetMonitorException {
        if (!Files.exists(file)) {
            if (legacyDevicesFile != null && Files.exists(legacyDevicesFile)) {
                return new AppData(new SerializedDeviceStore(legacyDevicesFile).load(), List.of());
            }
            return AppData.empty();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return (AppData) in.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new NetMonitorException("Impossible de lire le fichier de données " + file + ".", e);
        }
    }

    /** Écrit dans un fichier temporaire puis le renomme : un arrêt brutal ne corrompt pas les données. */
    @Override
    public void save(AppData data) throws NetMonitorException {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(data);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new NetMonitorException("Impossible d'enregistrer les données dans " + file + ".", e);
        }
    }
}
