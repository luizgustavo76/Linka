package com.LinkaProject.linkaLite;

public class Community {
    private String name;
    private String description;
    private String bannerUrl;
    private String iconUrl;
    public Community(String name, String description, String bannerUrl, String iconUrl) {
        this.name = name;
        this.description = description;
        this.bannerUrl = bannerUrl;
        this.iconUrl = iconUrl;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getBannerUrl() { return bannerUrl; }
    public String getIconUrl() { return iconUrl; }
}