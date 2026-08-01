package com.LinkaProject.linkaLite;

public class FederationItem {
    private String coverImage;
    private String description;
    private String name;
    private String url;

    public FederationItem(String coverImage, String description, String name, String url) {
        this.coverImage = coverImage;
        this.description = description;
        this.name = name;
        this.url = url;
    }

    public String getCoverImage() { return coverImage; }
    public String getDescription() { return description; }
    public String getName() { return name; }
    public String getUrl() { return url; }
}