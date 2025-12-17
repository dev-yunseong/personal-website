package dev.yunseong.website.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        String errorMessage = "An unexpected error occurred";
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            switch (statusCode) {
                case 404:
                    errorMessage = "Page not found";
                    break;
                case 403:
                    errorMessage = "Access forbidden";
                    break;
                case 500:
                    errorMessage = "Internal server error";
                    break;
                case 400:
                    errorMessage = "Bad request";
                    break;
                default:
                    errorMessage = "Error occurred (Status: " + statusCode + ")";
            }
            model.addAttribute("statusCode", statusCode);
        }

        if (message != null && !message.toString().isEmpty()) {
            model.addAttribute("errorDetails", message.toString());
        }

        model.addAttribute("errorMessage", errorMessage);
        return "error";
    }
}
