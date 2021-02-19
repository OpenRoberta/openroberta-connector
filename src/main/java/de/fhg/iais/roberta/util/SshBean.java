package de.fhg.iais.roberta.util;

import java.net.InetAddress;

public class SshBean {
    private InetAddress address;
    private String userName;
    private String password;
    private int port;

    public SshBean(InetAddress address, String userName, String password, int port) {
        this.address = address;
        this.userName = userName;
        this.password = password;
        this.port = port;
    }


    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "SshBean{" +
                "address=" + address +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", port=" + port +
                '}';
    }
}
