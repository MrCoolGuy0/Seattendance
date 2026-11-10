package com.seatendance;

import com.fazecast.jSerialComm.*;
import java.util.function.*; // para sa callback function

public class RFIDScanner {

    private final StringBuilder buffer = new StringBuilder();
    private SerialPort selectedPort;
    private Consumer<String> OnUIDSScanned;
    String lastUID = null;

    public void start() {
        SerialPort[] ports = SerialPort.getCommPorts();

        if (ports.length == 0) {
            System.out.println("No serial ports found. Make sure your Arduino is connected.");
            return;
        }

        System.out.println("Available serial ports:");
        for (int i = 0; i < ports.length; i++) {
            System.out.println(
                    "  [" + i + "] " + ports[i].getSystemPortName() + " - " + ports[i].getDescriptivePortName());
        }

        for (SerialPort port : ports) {
            String name = port.getDescriptivePortName().toLowerCase();
            if (name.contains("ch340") || name.contains("arduino")) {
                selectedPort = port;
                break;
            }
        }

        if (selectedPort == null) {
            System.out.println("No suitable port found.");
            return;
        }

        System.out.println("\nConnecting to: " + selectedPort.getSystemPortName());
        selectedPort.setBaudRate(9600);
        selectedPort.setNumDataBits(8);
        selectedPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        selectedPort.setParity(SerialPort.NO_PARITY);

        if (!selectedPort.openPort()) {
            System.out.println("Failed to open port. Check if the port is in use or try a different one.");
            return;
        }

        System.out.println("Port opened successfully. Waiting for RFID scans...\n");

        selectedPort.addDataListener(new SerialPortDataListener() {

            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                    return;

                byte[] data = new byte[selectedPort.bytesAvailable()];
                selectedPort.readBytes(data, data.length);

                for (byte b : data) {
                    char c = (char) b;
                    if (c == '\n') {
                        String line = buffer.toString().trim();
                        if (!line.isEmpty()) {
                            handleLine(line);
                        }
                        buffer.setLength(0);
                    } else if (c != '\r') {
                        buffer.append(c);
                    }
                }
            }
        });
    }

    public void stop() {
        if (selectedPort != null) {
            selectedPort.removeDataListener();
            selectedPort.closePort();
            System.out.println("Port closed. Goodbye!");
        }
    }

    // eto change para ma store ung string
    public void setOnUIDSScanned(Consumer<String> callback) {
        this.OnUIDSScanned = callback;
    }

    private void handleLine(String line) {
        System.out.println("[RAW] " + line);

        if (line.startsWith("RFID Tag UID:")) {

            lastUID = line.replace("RFID Tag UID:", "").trim().replace(" ", "").toUpperCase();

            System.out.println(">>> RFID Tag Scanned! UID: " + lastUID);

            if (OnUIDSScanned != null) {
                OnUIDSScanned.accept(lastUID);
            }

        }
    }

    public String getRFIDUID() {
        return lastUID;
    }
}