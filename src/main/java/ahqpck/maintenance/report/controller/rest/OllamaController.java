package ahqpck.maintenance.report.controller.rest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import ahqpck.maintenance.report.service.OllamaService;

@RestController
@RequestMapping("/api/ai")
public class OllamaController {

    @Autowired
    private OllamaService ollamaService;

    @PostMapping("/complaint-suggestion")
    public String getSuggestion(@RequestBody Map<String, String> request) {
        String description = request.get("description");
        String prompt = "Based on this complaint description, suggest a solution:\n\n" + description;
        return ollamaService.generateResponse("llama3", prompt);
    }
}