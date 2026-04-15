package proiect.io;

import java.io.*;

/**
 * Serializare/deserializare model. Cerinta 6 (proiect) cere salvarea
 * clasificatoarelor si a vectorilor pe disc.
 *
 * Folosim serializarea standard Java (ObjectOutputStream). Toate obiectele
 * salvate (SVMClassifier, Kernel, etc.) implementeaza Serializable.
 */
public final class ModelStore {

    private ModelStore() {}

    /** Salveaza orice obiect Serializable intr-un fisier binar. */
    public static void salveaza(Object obj, File f) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(f)))) {
            oos.writeObject(obj);
        }
    }

    /**
     * Incarca un obiect anterior salvat. Cast-ul la tipul concret se face
     * de apelant.
     */
    public static Object incarca(File f) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(f)))) {
            return ois.readObject();
        }
    }
}
