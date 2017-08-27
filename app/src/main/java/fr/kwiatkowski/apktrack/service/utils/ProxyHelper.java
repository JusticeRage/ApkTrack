package fr.kwiatkowski.apktrack.service.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import fr.kwiatkowski.apktrack.ui.SettingsFragment;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class ProxyHelper {

    /**
     * This function creates the right proxy object based on the settings
     * @param ctx The context of the application.
     * @return A Proxy object or null.
     */
    public static Proxy get_proxy(Context ctx)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String type = prefs.getString(SettingsFragment.KEY_PREF_PROXY_TYPE, "DIRECT");
        String address = prefs.getString(SettingsFragment.KEY_PREF_PROXY_ADDRESS, "127.0.0.1:9050");

        if (!test_proxy_address(address)) {
            return null;
        }
        String hostname = address.substring(0, address.lastIndexOf(':'));
        String port = address.substring(address.lastIndexOf(':') + 1);

        switch (type)
        {
            case "DIRECT":
                return Proxy.NO_PROXY; // No proxy.
            case "HTTP":
                return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostname, Integer.parseInt(port)));
            case "SOCKS":
                return new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(hostname, Integer.parseInt(port)));
            default:
                return Proxy.NO_PROXY;
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Verifies than an input proxy address is valid. Tests performed are:
     * - There should at least be one ':' separator in the url
     * - The port number needs to be an integer inferior to 65565.
     * - The address must not end with ":".
     * @param address The URL of the proxy server to connect to.
     * @return Whether the address can be parsed.
     */
    public static boolean test_proxy_address(String address)
    {
        if (address == null || address.length() == 0) {
            return false;
        }

        String[] splitted = address.split(":");
        if (splitted.length < 2 || address.charAt(address.length() - 1) == ':') {
            return false;
        }
        try
        {
            int port = Integer.parseInt(splitted[splitted.length-1]);
            if (port < 0 || port > 65565) {
                return false;
            }
        }
        catch (NumberFormatException e) { // Port number is not an integer!
            return false;
        }
        return true;
    }
}
