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
                // Arquivo ainda não existe
            }

            StringBuilder newContent = new StringBuilder();
            String headerSection = "[" + targetSection + "]";
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

    public void createEmptyFile(Context context, String filename) {
        try {
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.close();
        } catch (Exception e) {
        }
    }

    // Lê o formato INI do Linka e converte com precisão para JSON String
    public String loadCfgAsJson(Context context, String filename) {
        try {
            FileInputStream fis = context.openFileInput(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            String section = "";
            StringBuilder json = new StringBuilder();
            json.append("{");
            boolean firstSection = true;
            boolean hasKeysInSection = false;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                
                // Pula comentários ou linhas que começam com JSON antigo estragado
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";") || line.startsWith("{")) {
                    continue;
                }
                
                // Encontrou Cabeçalho de Seção [SECAO]
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
                    hasKeysInSection = false;
                    continue;
                }

                // Encontrou Chave=Valor
                int eq = line.indexOf("=");
                if (eq > 0 && section.length() > 0) {
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    value = value.replace("\\", "\\\\").replace("\"", "\\\"");
                    json.append("\"").append(key).append("\":\"").append(value).append("\",");
                    hasKeysInSection = true;
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

    // CORRIGIDO: Gera o padrão padrão do Linka em formato INI/CFG Puro
    public String createDefaultConfig(Context context, String fileName) {
        try {
            StringBuilder iniBuilder = new StringBuilder();
            
            iniBuilder.append("[SERVER]\n");
            iniBuilder.append("url=http://linkaProject.pythonanwhere.com\n\n");
            
            iniBuilder.append("[FAST_LOGIN]\n");
            iniBuilder.append("username=\n");
            iniBuilder.append("password=\n");
            iniBuilder.append("token_session=\n");

            String iniString = iniBuilder.toString();

            // Salva no armazenamento interno em formato INI
            saveCfg(context, fileName, iniString);

            // Retorna o JSON gerado a partir do INI para manter o retorno original do método
            return loadCfgAsJson(context, fileName);

        } catch (Exception e) {
            e.printStackTrace();
            return "";
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