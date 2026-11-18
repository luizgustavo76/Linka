package com.LinkaProject.linkaLite;

public class FederationItem {
    private String coverImage;
    private String description;
    private String name;
    private String url;
    private boolean isChecked;

    public FederationItem() {
    }

    public FederationItem(String coverImage, String description, String name, String url) {
        this.coverImage = coverImage;
        this.description = description;
        this.name = name;
        this.url = url;
        this.isChecked = false;
    }

    public String getCoverImage() { return coverImage; }
    public String getDescription() { return description; }
    public String getName() { return name; }
    public String getUrl() { return url; }

    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { this.isChecked = checked; }
}