package com.bedroid.beEx.entity;

public class People {
    private String m_name;
    private String m_email;

    public People(String name, String email) {
        this.m_name = name;
        this.m_email = email;
    }

    public String getName() {
        return m_name;
    }

    public void setName(String m_name) {
        this.m_name = m_name;
    }

    public String getEmail() {
        return m_email;
    }

    public void setEmail(String m_email) {
        this.m_email = m_email;
    }

    @Override
    public String toString() {
        String addressPart;

        if (null == this.getEmail() || this.getEmail().isEmpty()) {
            return "";
        }

        if (null != this.getName() && !this.getName().isEmpty()) {
            return this.getName() + " <" + getEmail() + ">";
        } else {
            return getEmail();
        }
    }
}
