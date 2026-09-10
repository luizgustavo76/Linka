package com.LinkaProject.linkaLite;
public class Group {
    private int id;
    private String name;
    private String permissions;
    public Group(int id, String name, String permissions) {
        this.id = id;
        this.name = name;
        this.permissions = permissions;
    }
    public int getId() { return id; }
    public String getName() { return name; }
    public String getPermissions() { return permissions; }
}