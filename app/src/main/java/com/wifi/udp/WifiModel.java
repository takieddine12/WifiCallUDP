package com.wifi.udp;

import java.net.InetAddress;

public class WifiModel {
    InetAddress inetAddress;
    String contact;


    public WifiModel(InetAddress inetAddress, String contact) {
        this.inetAddress = inetAddress;
        this.contact = contact;
    }


    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
}
