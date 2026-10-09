package com.LinkaProject.linkaLite;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class config {

    public String saveCfg(Context context, String filename, String content) {
        try {
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(content);
            writer.close();
            return "successful!";
        } catch (Exception e) {
            return e.toString();
        }
    }

    public static boolean configFileExists(Context context, String fileName) {
        File file = context.getFileStreamPath(fileName);
        return file != null && file.exists();
    }

    public String updateCfg(Context context, String filename, String targetSection, String newKey, String newValue) {
        try {
            String currentContent = "";
            try {
                FileInputStream fis = context.openFileInput(filename);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                currentContent = sb.toString();
            } catch (Exception e) {
            }

            String cleanSection = targetSection.replace("[", "").replace("]", "");
            String headerSection = "[" + cleanSection + "]";
            
            StringBuilder newContent = new StringBuilder();
            boolean sectionFound = false;
            boolean keyUpdated = false;
            String[] lines = currentContent.split("\n");

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.equalsIgnoreCase(headerSection)) {
                    sectionFound = true;
                    newContent.append(line).append("\n");
                    continue;
                }
                if (sectionFound && !keyUpdated && trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    newContent.append(newKey).append("=").append(newValue).append("\n");
                    keyUpdated = true;
                }
                if (sectionFound && !keyUpdated && trimmed.contains("=")) {
                    int eq = trimmed.indexOf("=");
                    String key = trimmed.substring(0, eq).trim();
                    if (key.equalsIgnoreCase(newKey)) {
                        newContent.append(newKey).append("=").append(newValue).append("\n");
                        keyUpdated = true;
                        continue;
                    }
                }
                newContent.append(line).append("\n");
            }

            if (sectionFound && !keyUpdated) {
                newContent.append(newKey).append("=").append(newValue).append("\n");
            }

            if (!sectionFound) {
                if (newContent.length() > 0 && !newContent.toString().endsWith("\n")) {
                    newContent.append("\n");
                }
                newContent.append(headerSection).append("\n");
                newContent.append(newKey).append("=").append(newValue).append("\n");
            }

            return saveCfg(context, filename, newContent.toString().trim());
        } catch (Exception e) {
            return e.toString();
        }
    }

    public String loadCfgAsJson(Context context, String filename) {
        if (!configFileExists(context, filename)) {
            if (filename.contains("timeline")) {
                return createDefaultConfigTimeline(context, filename);
            }
            return createDefaultConfig(context, filename);
        }

        try {
            FileInputStream fis = context.openFileInput(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            String section = "";
            StringBuilder json = new StringBuilder();
            json.append("{");
            boolean firstSection = true;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";") || line.startsWith("{")) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    if (!firstSection) {
                        if (json.charAt(json.length() - 1) == ',') {
                            json.deleteCharAt(json.length() - 1);
                        }
                        json.append("},");
                    }
                    section = line.substring(1, line.length() - 1);
                    json.append("\"").append(section).append("\":{");
                    firstSection = false;
                    continue;
                }
                int eq = line.indexOf("=");
                if (eq > 0 && section.length() > 0) {
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    value = value.replace("\\", "\\\\").replace("\"", "\\\"");
                    json.append("\"").append(key).append("\":\"").append(value).append("\",");
                }
            }

            if (json.charAt(json.length() - 1) == ',') {
                json.deleteCharAt(json.length() - 1);
            }
            if (!firstSection) {
                json.append("}");
            }
            json.append("}");
            br.close();

            return json.toString();

        } catch (Exception e) {
            if (filename.contains("timeline")) {
                return createDefaultConfigTimeline(context, filename);
            }
            return createDefaultConfig(context, filename);
        }
    }

    public String createDefaultConfig(Context context, String fileName) {
        try {
            StringBuilder iniBuilder = new StringBuilder();
            iniBuilder.append("[SERVER]\n");
            iniBuilder.append("url=http://linkaProject.pythonanywhere.com\n\n");
            iniBuilder.append("[FAST_LOGIN]\n");
            iniBuilder.append("username=\n");
            iniBuilder.append("password=\n");
            iniBuilder.append("token_session=\n");
            
            String iniString = iniBuilder.toString();
            saveCfg(context, fileName, iniString);
            return loadCfgAsJson(context, fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String createDefaultConfigTimeline(Context context, String fileName) {
        try {
            StringBuilder iniBuilder = new StringBuilder();
            iniBuilder.append("[FEDERATION-TIMELINE]\n");
            iniBuilder.append("urls=\n");
            iniBuilder.append("[FILTERS]\n");
            iniBuilder.append("tags=\n");
            
            String iniString = iniBuilder.toString();
            saveCfg(context, fileName, iniString);
            
            return loadCfgAsJson(context, fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public List<String> getSavedUrls(Context context) {
        List<String> list = new ArrayList<String>();
        try {
            String rawTimeline = loadCfgAsJson(context, "config-timeline.cfg");
            if (rawTimeline != null && !rawTimeline.isEmpty()) {
                JSONObject jsonTimeline = new JSONObject(rawTimeline);
                JSONObject fedSection = jsonTimeline.optJSONObject("FEDERATION-TIMELINE");
                if (fedSection != null) {
                    String urlsValue = fedSection.optString("urls", "");
                    if (!urlsValue.isEmpty()) {
                        String[] parts = urlsValue.split(",");
                        for (String part : parts) {
                            String clean = part.trim();
                            if (!clean.isEmpty() && !list.contains(clean)) {
                                list.add(clean);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void toggleUrl(Context context, String targetUrl) {
        if (targetUrl == null || targetUrl.trim().isEmpty()) {
            return;
        }
        String cleanUrl = targetUrl.trim();
        List<String> current = getSavedUrls(context);
        if (current.contains(cleanUrl)) {
            current.remove(cleanUrl);
        } else {
            current.add(cleanUrl);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < current.size(); i++) {
            sb.append(current.get(i));
            if (i < current.size() - 1) {
                sb.append(",");
            }
        }
        updateCfg(context, "config-timeline.cfg", "FEDERATION-TIMELINE", "urls", sb.toString());
    }

    // --- MÉTODOS PARA O CAMPO [FILTERS] ---

    public List<String> getSavedFilters(Context context) {
        List<String> list = new ArrayList<String>();
        try {
            String rawTimeline = loadCfgAsJson(context, "config-timeline.cfg");
            if (rawTimeline != null && !rawTimeline.isEmpty()) {
                JSONObject jsonTimeline = new JSONObject(rawTimeline);
                JSONObject filterSection = jsonTimeline.optJSONObject("FILTERS");
                if (filterSection != null) {
                    String tagsValue = filterSection.optString("tags", "");
                    if (!tagsValue.isEmpty()) {
                        String[] parts = tagsValue.split(",");
                        for (String part : parts) {
                            String clean = part.trim();
                            if (!clean.isEmpty() && !list.contains(clean)) {
                                list.add(clean);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addFilter(Context context, String tag) {
        if (tag == null || tag.trim().isEmpty()) return;
        String cleanTag = tag.trim().replace(",", ""); // evita quebrar o CSV
        List<String> current = getSavedFilters(context);
        if (!current.contains(cleanTag)) {
            current.add(cleanTag);
            saveFiltersList(context, current);
        }
    }

    public void removeFilter(Context context, String tag) {
        if (tag == null || tag.trim().isEmpty()) return;
        String cleanTag = tag.trim();
        List<String> current = getSavedFilters(context);
        if (current.contains(cleanTag)) {
            current.remove(cleanTag);
            saveFiltersList(context, current);
        }
    }

    private void saveFiltersList(Context context, List<String> current) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < current.size(); i++) {
            sb.append(current.get(i));
            if (i < current.size() - 1) {
                sb.append(",");
            }
        }
        updateCfg(context, "config-timeline.cfg", "FILTERS", "tags", sb.toString());
    }

    public String deleteFileLinka(Context context, String filename) {
        try {
            File file = context.getFileStreamPath(filename);
            if (file.exists()) {
                return file.delete() ? "deleted" : "failed";
            }
            return "not_found";
        } catch (Exception e) {
            return e.toString();
        }
    }
}