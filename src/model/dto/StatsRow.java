package model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatsRow {
    private String category;
    private long total;
    private long newCards;
    private long learning;
    private long master;
}