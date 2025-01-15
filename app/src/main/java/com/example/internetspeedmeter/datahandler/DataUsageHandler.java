package com.example.internetspeedmeter.datahandler;

public class DataUsageHandler
{
    private String type;
    private String usage;

    public DataUsageHandler(String type, String usage)
    {
        this.type = type;
        this.usage = usage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

}
