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