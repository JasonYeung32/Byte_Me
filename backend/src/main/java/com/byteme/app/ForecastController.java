import java.util.*;
import java.io.*;

public class ForecastController {

    public List<String> reservationPredictionEMA() throws IOException, InterruptedException {
        // Creating the python process
        File file = new File("ExponentialMovingAverage.py");
        String path = file.getAbsolutePath();
        ProcessBuilder processBuilder = new ProcessBuilder("python", "-u", path);

        // Waiting for the forecasting to finish
        Process process = processBuilder.start();
        process.waitFor();

        int exitCode = process.waitFor();
        System.out.println("Exit code: " + exitCode);

        // Creating a reader for the process results
        List<String> results = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        // Reading all result lines
        String line = "";
        while ((line = reader.readLine()) != null) {
            results.add(line);
        }

        return results;
    }

    public static void main(String args[]) throws IOException, InterruptedException{
        ForecastController controller = new ForecastController();
        List<String> results = controller.reservationPredictionEMA();
        System.out.println(results);
    }
}