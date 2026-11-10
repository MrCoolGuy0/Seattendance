package com.seatendance;

import java.util.function.Consumer;

public class RFIDManager {

    private final RFIDScanner scanner;
    private Consumer<String> currentListener;

    public RFIDManager() {
        this.scanner = new RFIDScanner();
    }

    public void start() {
        scanner.start();
        scanner.setOnUIDSScanned(this::handleScan);
    }

    public void stop() {
        scanner.stop();
    }

    public void setScanListener(Consumer<String> listener) {
        this.currentListener = listener;
    }

    private void handleScan(String uid) {
        if (currentListener != null) {
            currentListener.accept(uid);
        }
    }

    public boolean isAvailable() {
        return scanner != null;
    }

    public RFIDScanner getScanner() {
        return scanner;
    }
}

// basically what this do is its stop listening to the arduino
// (rfidScanner.java)
// when the log in page is already passed it will stop
// it will open again when the start session is ready safety net lang to para
// lang ma avoid pag pinaglaruan ng panel

// inoptimize ko narin ung scanning sa log in imbes na hiwalay ung scanning
// like mag search sya ng separate if admin or teacher now isa nalang sya so
// imbes na dalawa iniscan nya isa nalang
// inayos ko nadin pinaltan ko ung error lable sa log in para di nag frefreeze
// sa log in di na sya jOption pane kase pansin ko nag stutter ung slide sa
// gilid pag iniispam
// tsaka avoid narin ung pindot ng pindot ng ok