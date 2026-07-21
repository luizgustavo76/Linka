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
    }
    public String updateCfg(Context context, String filename, String targetSection, String newKey, String newValue) {
        try {
            // 1. Lê o arquivo atual (se não existir, inicia vazio)
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
                // Arquivo ainda não existe, vai criar um novo
            }

            StringBuilder newContent = new StringBuilder();
            String headerSection = "[" + targetSection + "]";
            boolean sectionFound = false;
            boolean keyUpdated = false;

            String[] lines = currentContent.split("\n");

            for (String line : lines) {
                String trimmed = line.trim();

                // Se achou a seção desejada
                if (trimmed.equalsIgnoreCase(headerSection)) {
                    sectionFound = true;
                    newContent.append(line).append("\n");
                    continue;
                }

                // Se achou outra seção depois da que a gente queria, e ainda não adicionou a chave nova
                if (sectionFound && !keyUpdated && trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    newContent.append(newKey).append("=").append(newValue).append("\n");
                    keyUpdated = true;
                }

                // Se está dentro da seção e achou a chave existente, atualiza o valor
                if (sectionFound && !keyUpdated && trimmed.contains("=")) {
                    int eq = trimmed.indexOf("=");
                    String key = trimmed.substring(0, eq).trim();
                    if (key.equalsIgnoreCase(newKey)) {
                        newContent.append(newKey).append("=").append(newValue).append("\n");
                        keyUpdated = true;
                        continue; // Pula a linha antiga
                    }
                }

                newContent.append(line).append("\n");
            }

            // Se a seção existia mas a chave era nova, insere no final da seção
            if (sectionFound && !keyUpdated) {
                newContent.append(newKey).append("=").append(newValue).append("\n");
            }

            // Se a seção nem existia, cria a seção e a chave no final do arquivo
            if (!sectionFound) {
                if (newContent.length() > 0 && !newContent.toString().endsWith("\n")) {
                    newContent.append("\n");
                }
                newContent.append(headerSection).append("\n");
                newContent.append(newKey).append("=").append(newValue).append("\n");
            }

            // 2. Salva o arquivo atualizado
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
    private String createDefaultConfig(Context context, String fileName) {
        try {
            JSONObject defaultConfig = new JSONObject();

            JSONObject fastLogin = new JSONObject();
            fastLogin.put("username", "");
            fastLogin.put("password", "")
            fastLogin.put("token_session", "");

            JSONObject server = new JSONObject();
            server.put("url", "http://linkaProject.pythonanwhere.com");

            defaultConfig.put("FAST_LOGIN", fastLogin);
            defaultConfig.put("SERVER", server);

            String jsonString = defaultConfig.toString();

            // Salva no armazenamento interno do Android
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(jsonString.getBytes());
            fos.close();

            return jsonString;

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
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