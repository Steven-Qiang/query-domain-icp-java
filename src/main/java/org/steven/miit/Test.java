package org.steven.miit;

import java.util.Scanner;

public class Test {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Miit miit = new Miit();
        while (true) {
            try {
                System.out.print("请输入要查询的主体: ");
                String unitName = sc.nextLine();
                System.out.println("查询结果如下：");
                QueryResponse queryResponse = miit.queryByUnitName(unitName);
                for (QueryResponse.ListDTO listDTO : queryResponse.getList()) {

                    String domain_content_approved = listDTO.getContentTypeName(),
                            domain_owner = listDTO.getUnitName(),
                            domain_name = listDTO.getDomain(),
                            domain_type = listDTO.getNatureName(),
                            domain_licence = listDTO.getMainLicence(),
                            domain_web_licence = listDTO.getServiceLicence(),
                            domain_site_name = listDTO.getLeaderName(),
                            domain_status = listDTO.getLimitAccess(),
                            domain_approve_date = listDTO.getUpdateRecordTime();

                    if (domain_content_approved.isEmpty()) domain_content_approved = "无";
                    System.out.println("\t域名主办方：" + domain_owner);
                    System.out.println("\t域名：" + domain_name);
                    System.out.println("\t网站名称：" + domain_site_name);
                    System.out.println("\t备案许可证号：" + domain_licence);
                    System.out.println("\t网站备案号：" + domain_web_licence);
                    System.out.println("\t域名类型：" + domain_type);
                    System.out.println("\t网站前置审批项：" + domain_content_approved);
                    System.out.println("\t是否限制接入：" + domain_status);
                    System.out.println("\t审核通过日期：" + domain_approve_date);
                }
                System.out.println("查询完毕\n\n");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
