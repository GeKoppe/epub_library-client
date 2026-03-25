package org.koppe.epub.client.dto;

import java.util.List;

import lombok.Data;

@Data
public class PagedRequestDto<T> {
    private Long number;
    private Long itemCount;
    private Long pageSize;
    private List<T> content;
}
