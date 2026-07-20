public String saveCfg(String filename, String content) {

    try {

        OutputStreamWriter writer =
                new OutputStreamWriter(
                        openFileOutput(
                                filename,
                                MODE_PRIVATE
                        )
                );

        writer.write(content);

        writer.close();

        return "successful!";

    } catch (Exception e) {

        e.printStackTrace();

        return e.toString();
    }
}
public void createEmptyFile(String filename) {

    try {

        FileOutputStream fos = openFileOutput(
                filename,
                MODE_PRIVATE
        );

        fos.close();

    } catch (Exception e) {
        e.printStackTrace();
    }
}
public String loadCfgAsJson(String filename) {

try {

    BufferedReader br = new BufferedReader(
            new InputStreamReader(
                    openFileInput(filename),
                    "UTF-8"
            )
    );

    String line;

    String section = "";

    StringBuilder json = new StringBuilder();

    json.append("{");

    boolean firstSection = true;

    while ((line = br.readLine()) != null) {

        line = line.trim();

        if (
                line.equals("") ||
                line.startsWith("#") ||
                line.startsWith(";")
        ) {

            continue;
        }

        if (
                line.startsWith("[") &&
                line.endsWith("]")
        ) {

            section = line.substring(
                    1,
                    line.length() - 1
            );

            if (!firstSection) {
                json.append("},");
            }

            json.append("\"")
                    .append(section)
                    .append("\":{");

            firstSection = false;

            continue;
        }

        int eq = line.indexOf("=");

        if (eq > 0 && section.length() > 0) {

            String key = line.substring(
                    0,
                    eq
            ).trim();

            String value = line.substring(
                    eq + 1
            ).trim();

            value = value
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");

            json.append("\"")
                    .append(key)
                    .append("\":\"")
                    .append(value)
                    .append("\",");
        }
    }

    br.close();

    if (json.charAt(json.length() - 1) == ',') {
        json.deleteCharAt(json.length() - 1);
    }

    if (!firstSection) {
        json.append("}");
    }

    json.append("}");

    return json.toString();

} catch (Exception e) {

    return "{\"ERROR\":\"" + e.toString() + "\"}";
}
}
public String deleteFileLinka(String filename) {

try {

    File file = getFileStreamPath(filename);

    if(file.exists()){

        boolean deleted = file.delete();

        if(deleted){
            return "DELETED";
        }else{
            return "FAILED";
        }

    }else{

        return "NOT_FOUND";
    }

} catch (Exception e) {

    return "ERROR:" + e.toString();
}
}