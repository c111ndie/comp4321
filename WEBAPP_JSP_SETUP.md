# JSP Integration Guide

## Overview
The webapp now uses a **JSP (JavaServer Pages) approach** to connect the frontend and backend. This setup allows:
- Dynamic page rendering on the server side
- Server-side data binding to the frontend
- Seamless integration between the search backend and UI

## Architecture

```
Request Flow:
    User Browser
        ↓
    Spring Boot (Embedded Tomcat)
        ↓
    PageController (routes requests)
        ↓
    JSP View (renders with model data)
        ↓
    HTML to Browser
        ↓
    JavaScript (frontend interactivity)
        ↓
    API calls to /api/search, /api/suggest (REST endpoints)
```

## File Structure

```
webapp/src/main/
├── java/
│   └── com/comp4321/webapp/
│       ├── WebApplication.java (Spring Boot entry point)
│       ├── controller/
│       │   └── PageController.java ✨ NEW - Handles page routing
│       └── api/
│           └── MockSearchController.java (REST API endpoints)
├── webapp/
│   └── WEB-INF/
│       └── jsp/
│           ├── index.jsp ✨ NEW - Home page
│           └── results.jsp ✨ NEW - Search results page
└── resources/
    ├── application.properties (updated with JSP config)
    ├── static/
    │   ├── css/
    │   ├── js/
    │   │   ├── home.js (home page logic)
    │   │   ├── results.js (results page logic)
    │   │   ├── autocomplete.js (search suggestions)
    │   │   ├── history.js (search history)
    │   │   └── saved.js (saved results)
    │   └── assets/
```

## How It Works

### 1. **Page Controller** (`PageController.java`)
Routes page requests and passes data to JSP views:

```java
@GetMapping({"/", "/index", "/index.jsp"})
public String index(Model model) {
    model.addAttribute("pageTitle", "HKUST Search Engine");
    return "index";  // Renders WEB-INF/jsp/index.jsp
}

@GetMapping({"/results", "/results.jsp"})
public String results(@RequestParam(name = "q", defaultValue = "") String query,
                      @RequestParam(name = "page", defaultValue = "1") int page,
                      Model model) {
    model.addAttribute("query", query);
    model.addAttribute("currentPage", page);
    return "results";  // Renders WEB-INF/jsp/results.jsp
}
```

### 2. **JSP Pages** (Dynamic Template Layer)
- **index.jsp**: Home page with search form (form submits to `/results`)
- **results.jsp**: Results page with:
  - Dynamic query display via `${query}`
  - Server-side model binding
  - Client-side JavaScript for result rendering

### 3. **REST API Layer** (`MockSearchController.java`)
- Provides `/api/search` endpoint for search queries
- Provides `/api/suggest` endpoint for autocomplete
- Returns JSON responses consumed by JavaScript

### 4. **Frontend JavaScript** (Client-side interactivity)
- `home.js`: Handles home page interactions
- `results.js`: Fetches from `/api/search` and renders results
- `autocomplete.js`: Handles search suggestions
- All APIs called via fetch/AJAX to `/api/*` endpoints

## Configuration Changes

**application.properties** now includes:
```properties
# JSP Configuration
spring.mvc.view.prefix=/WEB-INF/jsp/
spring.mvc.view.suffix=.jsp

# Static resources
spring.mvc.static-path-pattern=/static/**
spring.web.resources.static-locations=classpath:/static/
```

## Running the Application

### 1. **Build**
```bash
./mvnw clean install
```

### 2. **Run**
```bash
# From project root
java -jar webapp/target/webapp-1.0.0.jar
```

Or with search backend:
```bash
java \
  -Dsearch.db-name=spider/crawl-output/indexDB \
  -Dsearch.stopwords=spider/src/main/resources/stopwords.txt \
  -jar webapp/target/webapp-1.0.0.jar
```

### 3. **Access**
- Home page: `http://localhost:8080/`
- Results page: `http://localhost:8080/results?q=university`

## Integration with Real Search Backend

When you're ready to integrate the real search backend (instead of mock data):

### 1. **Create a real SearchController**
```java
@RestController
@RequestMapping("/api")
public class SearchController {
    @Autowired
    private SearchService searchService;
    
    @GetMapping("/search")
    public SearchResponse search(@RequestParam String q) {
        return searchService.search(q);
    }
}
```

### 2. **Update properties**
```properties
# Disable mock search
webapp.mock-search.enabled=false

# Point to your JDBM database
search.db-name=spider/crawl-output/indexDB
search.stopwords=spider/src/main/resources/stopwords.txt
```

### 3. **Create SearchService**
```java
@Service
public class SearchService {
    public SearchResponse search(String query) {
        // Use Search class from spider module
        // Return results as JSON-compatible objects
    }
}
```

## URL Mapping Summary

| URL | Controller | View | Purpose |
|-----|-----------|------|---------|
| `/` | PageController.index() | index.jsp | Home page |
| `/index` | PageController.index() | index.jsp | Home page (alias) |
| `/results?q=...` | PageController.results() | results.jsp | Results page with query |
| `/api/search?q=...` | MockSearchController.search() | JSON | Search API endpoint |
| `/api/suggest?q=...` | MockSearchController.suggest() | JSON | Autocomplete API endpoint |
| `/api/keywords` | MockSearchController.keywords() | JSON | Keyword catalog API |
| `/static/**` | (static resource) | - | CSS, JS, images |

## Next Steps

1. **Connect Real Search Backend**: Replace `MockSearchController` with real search implementation
2. **Add More Features**:
   - Query refinement (filters, sorting)
   - Search history persistence
   - Saved results management
   - Advanced search syntax
3. **Optimize Performance**:
   - Add result caching
   - Implement pagination
   - Add search result ranking improvements
4. **Testing**:
   - Unit tests for controllers
   - Integration tests for search functionality
   - Frontend tests for JS interactions

## Resource References

- Spring Boot MVC: https://spring.io/guides/gs/serving-web-content/
- JSP with Spring: https://spring.io/guides/gs/spring-boot-with-the-webapp/
- JSTL Core Tags: https://docs.oracle.com/javaee/5/jstl/1.1/docs/tlddocs/
