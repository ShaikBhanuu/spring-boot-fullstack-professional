package com.example.demo.site;

import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;

    public List<Site> getAllSites() {
        return siteRepository.findAll();
    }

    public Site createSite(Site site) {
        return siteRepository.save(site);
    }

    public Site updateSite(Long id, Site updated) {
        Site existing = siteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Site not found with id: " + id));

        existing.setSiteName(updated.getSiteName());
        existing.setLocation(updated.getLocation());
        existing.setActive(updated.getActive());

        return siteRepository.save(existing);
    }
}