package com.pixellink.controller;

import com.pixellink.service.LinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {DashboardController.class, AdminController.class})
public class GlobalModelAdvice {

    @Autowired
    private LinkService linkService;

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("customSlugEnabled", "true".equals(linkService.getSystemSetting("feature_custom_slug_enabled", "true")));
        model.addAttribute("seoPreviewEnabled", "true".equals(linkService.getSystemSetting("feature_seo_preview_enabled", "true")));
        model.addAttribute("smartRoutingEnabled", "true".equals(linkService.getSystemSetting("feature_smart_routing_enabled", "true")));
        model.addAttribute("monetizationEnabled", "true".equals(linkService.getSystemSetting("feature_monetization_enabled", "true")));
        model.addAttribute("marketingPixelEnabled", "true".equals(linkService.getSystemSetting("feature_marketing_pixel_enabled", "true")));
    }
}
