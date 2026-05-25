package com.example.demo.site;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/sites")
@AllArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @GetMapping
    public List<Site> getAllSites() {
        return siteService.getAllSites();
    }

    @PostMapping
    public ResponseEntity<Site> createSite(
            @RequestBody Site site) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(siteService.createSite(site));
    }

    @PutMapping("{id}")
    public Site updateSite(
            @PathVariable Long id,
            @RequestBody Site site) {
        return siteService.updateSite(id, site);
    }
}