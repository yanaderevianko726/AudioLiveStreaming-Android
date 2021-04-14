package co.mobiwise.library.radio;


public interface IRadioManager {

    void startRadio(String streamURL);

    void stopRadio();

    void registerListener(RadioListener mRadioListener);

    void connect();

    void disconnect();

}