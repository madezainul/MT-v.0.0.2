package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.config.UserDetailsImpl;
import ahqpck.maintenance.report.dto.PartDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.service.PartService;
import ahqpck.maintenance.report.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/parts")
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;
    private final UserService userService;

    @GetMapping
    public String listParts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc,
            Authentication authentication,
            Model model) {

        try {
            String currentUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                currentUserId = userDetails.getId();
            }

            // Only fetch current user if needed
            if (currentUserId != null) {
                UserDTO currentUser = userService.getUserById(currentUserId);
                model.addAttribute("currentUser", currentUser);
            }

            var partsPage = partService.getAllParts(keyword, page, size, sortBy, asc);
            model.addAttribute("parts", partsPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);

            model.addAttribute("title", "Parts Inventory");
            model.addAttribute("sortFields", new String[] { "code", "name", "category", "supplier", "stockQuantity" });
            model.addAttribute("partDTO", new PartDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load parts: " + e.getMessage());
        }

        return "part/index";
    }

    @PostMapping
    public String createPart(
            @Valid @ModelAttribute PartDTO partDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            return "redirect:/parts";
        }

        try {
            partService.createPart(partDTO, imageFile);
            ra.addFlashAttribute("success", "Part created successfully.");
            return "redirect:/parts";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/parts";
        }
    }

    // === UPDATE ===
    @PostMapping("/update")
    public String updatePart(
            @Valid @ModelAttribute PartDTO partDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "deleteImage", required = false, defaultValue = "false") boolean deleteImage,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            return "redirect:/parts";
        }

        try {
            partService.updatePart(partDTO, imageFile, deleteImage);
            ra.addFlashAttribute("success", "Part updated successfully.");
            return "redirect:/parts";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/parts";
        }
    }

    @GetMapping("/delete/{id}")
    public String deletePart(@PathVariable String id, RedirectAttributes ra) {
        try {
            partService.deletePart(id);
            ra.addFlashAttribute("success", "Part deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/parts";
    }
}
