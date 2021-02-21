import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import service.XmlAnalyser;

import java.net.URL;
import java.util.List;

@Slf4j
public class Starter {
    @SneakyThrows
    public static void main(String[] args) {
        if (args.length < 3) {
            log.info("Invalid input. Please provide <input_origin_file_path> <input_other_sample_file_path> <target_element_id>");
            return;
        }
        XmlAnalyser xmlAnalyser = new XmlAnalyser();
        List<String> similarElements = xmlAnalyser.getSimilarElements(new URL("file:" + args[0]),
                new URL("file:" + args[1]), args[2]);
        if (similarElements.isEmpty()) {
            log.info("No elements similar to {} found", args[2]);
        }
        similarElements.forEach(se -> log.info("Result element - {}", se));
    }
}
