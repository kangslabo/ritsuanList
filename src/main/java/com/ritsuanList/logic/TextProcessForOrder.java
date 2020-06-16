package com.ritsuanList.logic;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TextProcessForOrder extends TextProcess {

    public TextProcessForOrder() {
        super();
    }

    public String
    textToObject(List<String> txtList) {
        return mapToJson(
                txtList.stream().anyMatch(s -> s.contains("プログラム作成")) ?
                        fromProgramOrder(txtList) : fromKeihiOrder(txtList)
        );
    }

    private Map<String, String> fromKeihiOrder(List<String> text) {
        Map<String, String> map = new HashMap<>();
        map.put("種別", "経費支出");
        map.put("立案番号", text.get(0).replace(" ", ""));
        map.put("納品期日", text.stream().filter(s -> s.contains("納品期日")).collect(Collectors.joining())
                .replace("２． 納品期日", "")
                .replace("まで", "")
                .replace(" ", ""));
        return map;
    }

    private Map<String, String> fromProgramOrder(List<String> text) {
        Map<String, String> map = new HashMap<>();

        map.put("種別", "受注");
        map.put("立案番号",
                StringUtils.trim(text.stream().filter(i -> i.contains("契 約 番 号 ")).findFirst().get()
                        .replace("契 約 番 号", "")));
        List<String> headers = Arrays.asList(properties.getProperty("programOrderHeader").split(","));

        headers.stream().forEach(s -> {
            String content = "";
            if (s.equals("( 支 払 方 法 )")) {
                map.put("支払方法",
                        StringUtils.trim(
                                text.stream().filter(i -> i.contains("支払方法")).findFirst().get()
                                        .replace("( 支 払 方 法 )", "")
                                        .replace("(", "").replace(")", "")
                        )
                );
            } else {
                long endHeader = (long) headers.indexOf(s) != headers.size() - 1 ?
                        (long) headers.indexOf(s) + 1 : (long) headers.indexOf(s);
                String endString = subIdx(text, headers.get((int) endHeader));
                int strIdx = text.indexOf(subIdx(text, s));
                int endIdx = endString.isEmpty() ? text.size() : text.indexOf(endString);
                if (strIdx > 0) {
                    content = text.stream().skip(strIdx).limit(endIdx - strIdx).collect(Collectors.joining(" "))
                            .replace(s, "");
                }
                switch (s) {
                    case "検査完了日":
                        map.put("契約金額", content.split("金")[1]);
                        content = content.split("金")[0];
                        map.put(s.replace(" ", ""), StringUtils.trim(content));
                        break;
                    case "契 約 金 額":
                        break;
                    case "開 発 期 間":
                        content = content.split("納品日")[1];
                        s = "納品期日";
                    default:
                        map.put(s.replace(" ", ""), StringUtils.trim(content));
                        break;
                }
            }
        });
        return map;
    }

}
