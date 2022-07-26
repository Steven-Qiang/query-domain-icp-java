package org.steven.miit;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class QueryResponse {

    private int endRow;
    private int firstPage;
    private boolean hasNextPage;
    private boolean hasPreviousPage;
    private boolean isFirstPage;
    private boolean isLastPage;
    private int lastPage;
    private List<ListDTO> list;
    private int navigatePages;
    private List<Integer> navigatepageNums;
    private int nextPage;
    private String orderBy;
    private int pageNum;
    private int pageSize;
    private int pages;
    private int prePage;
    private int size;
    private int startRow;
    private int total;

    @NoArgsConstructor
    @Getter
    public static class ListDTO {
        private String contentTypeName; // 网站前置审批项
        private String domain; // 域名
        private long domainId; // 域名ID
        private String leaderName; // 网站名称
        private String limitAccess; // 是否限制接入
        private int mainId; // 主体ID
        private String mainLicence; // 备案许可证号
        private String natureName; // 域名类型
        private int serviceId; // 备案ID
        private String serviceLicence; // 网站备案号
        private String unitName; // 域名主体
        private String updateRecordTime; // 审核通过日期
    }
}
