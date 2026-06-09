package com.yxshop.Module.Content.Dto;

import lombok.Data;

@Data
public class TopicDto {
    // Canonical fields (used by old API)
    private String title;
    private String coverImage;
    private String contentBlocks;
    private Integer status;

    // Frontend admin form fields (mapped: name→title, image→coverImage)
    private String name;
    private String description;
    private String image;
    private Boolean isHot;
    private Integer sort;

    /** Resolve the actual title: prefer name, fall back to title */
    public String resolvedTitle() {
        return (name != null && !name.trim().isEmpty()) ? name.trim()
                : (title != null ? title.trim() : null);
    }

    /** Resolve the actual coverImage: prefer image, fall back to coverImage */
    public String resolvedCoverImage() {
        return (image != null && !image.trim().isEmpty()) ? image.trim() : coverImage;
    }
}
