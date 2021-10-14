package org.luxoft.sdl_core;

import android.util.Log;

public class AndroidSettings {

    private static final String AndroidSection = "ANDROID";
    private static final String TAG = CompressionUtil.class.getSimpleName();

    private static int bufferSize = 131072;
    private static String readerSocketAdress = "./localBleReader";
    private static String controlSocketAdress = "./localBleControl";
    private static String writerSocketAdress = "./localBleWriter";
    private static int prefferredMtu = 512;
    private static String sdlTesterServiceUUID = "00001101-0000-1000-8000-00805f9b34fb";
    private static String mobileNotificationCharacteristic = "00001102-0000-1000-8000-00805f9b34fb";
    private static String mobileResponceCharacteristic = "00001104-0000-1000-8000-00805f9b34fb";

    private static int getIntValue(IniLoader iniFile, String section, String Key, int defaultValue) {
        int result = defaultValue;

        if (iniFile.containsKey(section, Key)) {
            String value = iniFile.getValue(section, Key);
            try {
                result = Integer.parseInt(value.trim());
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    private static String getStrValue(IniLoader iniFile, String section, String Key, String defaultValue) {
        String result = defaultValue;

        if(iniFile.containsKey(section, Key)) {
            result = iniFile.getValue(section, Key).trim().replaceAll("^\"+|\"+$", "");
        }

        return result;
    }

    public static  void loadSettings(String filepath) {
        IniLoader iniLoader = new IniLoader();
        iniLoader.load(filepath + "/androidSmartDeviceLink.ini");

        bufferSize = getIntValue(iniLoader, AndroidSection, "BufferSize", bufferSize);
        readerSocketAdress = getStrValue(iniLoader, AndroidSection, "ReaderSocketAdress", readerSocketAdress);
        controlSocketAdress = getStrValue(iniLoader, AndroidSection, "ControlSocketAdress", controlSocketAdress);
        writerSocketAdress = getStrValue(iniLoader, AndroidSection, "WriterSocketAdress", writerSocketAdress);
        prefferredMtu = getIntValue(iniLoader, AndroidSection, "PrefferredMtu", prefferredMtu);
        sdlTesterServiceUUID = getStrValue(iniLoader, AndroidSection, "SdlTesterServiceUUID", sdlTesterServiceUUID);
        mobileNotificationCharacteristic = getStrValue(iniLoader, AndroidSection, "MobileNotificationCharacteristic", mobileNotificationCharacteristic);
        mobileResponceCharacteristic = getStrValue(iniLoader, AndroidSection, "MobileResponceCharacteristic", mobileResponceCharacteristic);

    }

    public static int getBufferSize() {
        return bufferSize;
    }
    public static String getReaderSocketAddress() {
        return readerSocketAdress;
    }

    public static String getControlSocketAddress() {
        return controlSocketAdress;
    }

    public static String getWriterSocketAddress() {
            return writerSocketAdress;
    }

    public static int getPrefferredMtu(){
        return prefferredMtu;
    }

    public static String getSdlTesterServiceUUID() {
        return sdlTesterServiceUUID;
    }

    public static String getMobileNotificationCharacteristic() {
        return mobileNotificationCharacteristic;
    }

    public static String getMobileResponseCharacteristic() {
        return mobileResponceCharacteristic;
    }

}
