package ahqpck.maintenance.report.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class OllamaService {

    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateResponse(String model, String prompt) {
        try {
            // Build request body
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
            );

            // Call Ollama
            String response = restTemplate.postForObject(OLLAMA_API_URL, requestBody, String.class);

            // Parse JSON and extract "response"
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.get("response").asText();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get response from Ollama: " + e.getMessage(), e);
        }
    }
}