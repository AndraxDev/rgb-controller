package com.teslasoft.iot.rgbcontroller;

public class UserModel {
    private String user_name;
    private String user_email;
    private String is_dev;

    public UserModel(String user_name, String user_email, String is_dev) {
        this.user_name = user_name;
        this.user_email = user_email;
        this.is_dev = is_dev;
    }


    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public String getIs_dev() {
        return is_dev;
    }

    public void setIs_dev(String is_dev) {
        this.is_dev = is_dev;
    }
}
