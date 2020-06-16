package com.ritsuanList.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class TextProcess {

    private static final String FILE_PATH = "/setting.properties";
    protected static final Properties properties = new Properties();

    public TextProcess() {

        try {
            properties.load(this.getClass().getResourceAsStream(FILE_PATH));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String textToObject(List<String> text) {
        Map<String, String> ritsuanObject = new HashMap<>();

        ritsuanObject.put("立案番号", StringUtils.trim(text.get(1)).trim());
        ritsuanObject.put("立案", StringUtils.trim(text.get(2).split(" ")[2]));
        ritsuanObject.put("決裁", StringUtils.trim(text.get(3).split(" ")[2]));
        ritsuanObject.put("執行", StringUtils.trim(text.get(4).split(" ")[2]));
        ritsuanObject.put(
                "表題",
                text.stream().skip(8).limit(text.indexOf(subIdx(text, "立 案 箇 所")) - 8).collect(Collectors.joining())
        );
        ritsuanObject.put(
                "種別",
                ritsuanObject.get("表題").contains("【支出】") ? "請負(委託)" : "経費支出"
        );

        text.stream().skip(3).limit(3).forEach(statusAndDate -> {
                    statusAndDate = statusAndDate.replace(" ", "");
                    ritsuanObject.put(statusAndDate.replace(" ", "").substring(0, 2),
                            statusAndDate.contains("年") ?
                                    statusAndDate.substring(2, statusAndDate.indexOf("日") + 1) : ""
                    );
                }
        );

        List<String> headers = Arrays.asList(properties.getProperty("header").split(","));
        headers.stream().forEach(s -> {
            switch (s) {
                case "契 約 相 手 ":
                case "立 案 者 ":
                    ritsuanObject.put(
                            s.replace(" ", ""),
                            text.stream().filter(i -> i.contains(s)).collect(Collectors.joining()).replace(s, "")
                    );
                    break;
                default:
                    long endHeader = (long) headers.indexOf(s) != headers.size() - 1 ?
                            (long) headers.indexOf(s) + 1 : (long) headers.indexOf(s);
                    String endString = "";
                    String content = "";
                    try {
                        endString = s.equals("添 付 資 料") ?
                                attachedFile(text, headers.get((int) endHeader)) :
                                subIdx(text, headers.get((int) endHeader));
                    } catch (Exception ex) {
                        endString = s.equals("添 付 資 料") ?
                                attachedFile(text, headers.get((int) endHeader)) :
                                subIdx(text, headers.get((int) endHeader + 1));
                    }
                    int strIdx = text.indexOf(subIdx(text, s));
                    int endIdx = endString.isEmpty() ?
                            text.size() : text.indexOf(endString);
                    if (strIdx > 0) {
                        content = text.stream().skip(strIdx).limit(endIdx - strIdx).collect(Collectors.joining(" "))
                                .replace(s, "");
                    }
                    ritsuanObject.put(s.replace(" ", ""), content);
                    break;
            }
        });
        ritsuanObject.put("契約金額",ritsuanObject.get("契約金額").split("円")[2].split("税抜契約金額") [1].trim());
        ritsuanObject.put("分類", getType(ritsuanObject.get("契約内容")));

        String siharaiJouken = ritsuanObject.get("支払条件");
        String keiyakuKikan = ritsuanObject.get("契約期間");

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");


        Date kaishibi = null;
        Date manryoubi = null;
        try {
            kaishibi = simpleDateFormat.parse(keiyakuKikan.split("～")[0]);
            manryoubi  = simpleDateFormat.parse(keiyakuKikan.split("～")[1]);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        String addDays = "";
        String shiharaiKinichi = "";

        switch (siharaiJouken) {
            case "請求書受理日より60日以内":
            case "請求書受理日より30日以内":
                addDays = siharaiJouken.substring(8, 10);
                shiharaiKinichi = dateToShiharai(kaishibi, addDays);
                break;
            case "納品日から60日以内":
            case "納品日から30日以内":
                addDays = siharaiJouken.substring(5, 7);
                shiharaiKinichi = dateToShiharai(manryoubi, addDays);
                break;
            case "月末締め翌月末払い":
            case "月末締め翌々月末払い":
                if(siharaiJouken.contains("翌々月")){
                    addDays = "2";
                } else{
                    addDays = "1";
            }
                shiharaiKinichi = dateToShiharai(kaishibi, addDays);
                break;
        }

        ritsuanObject.put("支払期限", shiharaiKinichi);

        if (ritsuanObject.get("支払条件").equals("納品日から60日以内")) {
            ritsuanObject.put("下請法", "対象");
        } else {
            ritsuanObject.put("下請法", "対象外");
        }
        return mapToJson(ritsuanObject);
    }

    protected String getType(String ritsuanNaiyou) {
        // 調整必要
        String type = ritsuanNaiyou.contains("月額")?
                "月払い" : "一回払い";
        return type;
    }

    protected String subIdx(List<String> textList, String sub) {
        return textList.stream().filter(i -> i.contains(sub)).count() > 1 ?
                textList.stream().filter(i -> i.contains(sub)).findFirst().get() :
                textList.stream().filter(i -> i.contains(sub)).collect(Collectors.joining());
    }

    protected String attachedFile(List<String> textList, String sub) {
        Long count = textList.stream().count();
        return textList.stream().filter(i -> i.contains(sub)).count() > 1 ?
                textList.stream().filter(i -> i.contains(sub)).skip(count - 1).findFirst().get() :
                subIdx(textList, sub);
    }

    protected String dateToShiharai(Date date, String addDays) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        switch (addDays) {
            case "1":
            case "2":
                cal.add(Calendar.MONTH, Integer.parseInt(addDays));
                cal.set(Calendar.DATE,cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                break;
            default:
                cal.add(Calendar.DAY_OF_MONTH, Integer.parseInt(addDays));
                cal.set(Calendar.DATE,cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                break;
        }

        return simpleDateFormat.format(cal.getTime());
    }

    protected String mapToJson(Map<String, String> map) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String script = "";
        try {
            script = objectMapper.writeValueAsString(map);
        } catch (IOException ie) {
            ie.printStackTrace();
        }
        return script;
    }


}
