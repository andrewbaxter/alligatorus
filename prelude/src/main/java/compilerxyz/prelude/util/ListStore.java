package compilerxyz.prelude.util;

public interface ListStore {
    int listStoreSize();

    Object listStoreBoxed(int index);

    void listStoreAddBoxed(Object value);

    ListStore listStoreCopy();
}
