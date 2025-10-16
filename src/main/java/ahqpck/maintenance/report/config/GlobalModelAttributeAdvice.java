package ahqpck.maintenance.report.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Global controller advice to provide common model attributes for all controllers
 */
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalModelAttributeAdvice {

    private final UserService userService;

    /**
     * Adds the current user to the model for all controllers
     * This ensures that the dashboard layout always has access to currentUser
     */
    @ModelAttribute("currentUser")
    public UserDTO addCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())
                && authentication.getPrincipal() instanceof UserDetailsImpl) {
                
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                String currentUserId = userDetails.getId();
                
                if (currentUserId != null) {
                    return userService.getUserById(currentUserId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get current user for global model attribute: {}", e.getMessage());
        }
        
        // Return null if no authenticated user or error occurred
        // The template will handle this with the fallback user profile
        return null;
    }
}