package com.unifiapp.adapter;

import java.util.ArrayList;
import java.util.List;

public class Group
{
    public String string;
    public final List<String> children = new ArrayList<String>();
    public String profile_picture_url;
    public String points;
    public Boolean placeholder;

    public Group(String string, Boolean placeholder)
    {
        this.string = string;
        this.placeholder = placeholder;
    }

}