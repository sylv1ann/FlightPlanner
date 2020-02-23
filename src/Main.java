import java.io.Console;

public class Main {

    public static void main(String[] args) {
        //Downloader.downloadMETAR(new String[]{"LZKZ", "LZIB"});
        //Downloader.downloadMETAR((String[]) null);

    }

    /*

     prompt to invoke //remove(new File("C:\\Users\\vikto\\Downloads\\airports1.csv"));

    public static void remove(File file) {
        File outputFile = new File("C:\\Users\\vikto\\Downloads\\airports2.csv");
        try {
            if (outputFile.createNewFile()) {
                try (InputStreamReader is = new InputStreamReader(new FileInputStream(file));
                     BufferedReader br = new BufferedReader(is);
                     OutputStreamWriter wr = new OutputStreamWriter(new FileOutputStream(outputFile))) {
                    String line;
                    int lineNumber = 0;

                    while ((line = br.readLine()) != null) {
                        ++lineNumber;
                        String[] fields = line.split(",", 0);
                        System.out.println("Printing line: " + lineNumber);
                        for (String field : fields) {
                            wr.append(field).append(",");
                        }
                        wr.append("\n");
                    }
                } catch (IOException ignored) { }
            }
        } catch (IOException ignored) { }
    }*/
}
