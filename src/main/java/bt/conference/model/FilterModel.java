package bt.conference.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterModel {
    String searchString;

    String sortBy;

    int pageIndex;

    int pageSize;
}
