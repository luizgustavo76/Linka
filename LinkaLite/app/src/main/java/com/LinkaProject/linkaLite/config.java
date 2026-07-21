package com.LinkaProject.linkaLite;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

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

    public void createEmptyFile(Context context, String filename) {
        try {
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.close();
        } catch (Exception e) {
        }
    }

    public String loadCfgAsJson(Context context, String filename) {
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
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    if (!firstSection) {
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
                    value = value.replace("\"", "\\\"");
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
            return "{}";
        }
    }

    public String deleteFileLinka(Context context, String filename) {
        try {
            File file = context.getFileStreamPath(filename);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    return "deleted";
                } else {
                    return "failed";
                }
            }
            return "not_found";
        } catch (Exception e) {
            return e.toString();
        }
    }
}