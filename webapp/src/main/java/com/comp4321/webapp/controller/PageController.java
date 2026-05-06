package com.comp4321.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Spring MVC Controller for serving JSP pages.
 * Handles page routing and passes data to the view layer.
 */
@Controller
public class PageController {

    /**
     * Serves the home/index page.
     */
    @GetMapping({"/", "/index", "/index.jsp"})
    public String index(Model model) {
        model.addAttribute("pageTitle", "HKUST Search Engine");
        return "index";
    }

    /**
     * Serves the search results page.
     * 
     * @param query the search query string
     * @param page  the page number (1-indexed, default 1)
     * @param model the Spring model to pass data to the view
     */
    @GetMapping({"/results", "/results.jsp"})
    public String results(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {
        model.addAttribute("query", query);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageTitle", "Search Results - HKUST Search Engine");
        return "results";
    }
}
