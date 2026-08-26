package com.nba.auth;

import com.nba.core.exception.invalidData.UserInvalidDataException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class WebAuthController {

    private final AuthService authService;

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }


    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }


    @PostMapping("/register")
    public String registerUser(RegisterRequest request, Model model) {
        try {
            authService.register(request);

            return "redirect:/login?registered=true";
        } catch (UserInvalidDataException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}